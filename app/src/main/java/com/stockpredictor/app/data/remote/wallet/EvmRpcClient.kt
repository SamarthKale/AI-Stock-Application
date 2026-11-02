package com.stockpredictor.app.data.remote.wallet

import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** One non-zero balance read from a chain. [token] is null for the chain's native coin. */
data class RawBalance(val chain: EvmChain, val token: Erc20Token?, val amount: BigDecimal)

class EvmRpcException(message: String, cause: Throwable? = null) : Exception(message, cause)

private val ADDRESS_REGEX = Regex("^0x[0-9a-fA-F]{40}$")
fun isValidEvmAddress(address: String): Boolean = ADDRESS_REGEX.matches(address)

/**
 * Minimal read-only EVM JSON-RPC client: for each chain it sends ONE batched request containing
 * `eth_getBalance` (native coin) plus an `eth_call balanceOf(address)` per registry token, so a
 * whole chain costs a single HTTP round trip. Read-only by construction -- it only ever calls
 * view methods with a public address; no keys, no signing, no transactions.
 */
class EvmRpcClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMedia = "application/json".toMediaType()

    /** Non-zero balances for [address] on [chain]. Throws [EvmRpcException] if the chain's RPC fails. */
    suspend fun fetchBalances(chain: EvmChain, address: String): List<RawBalance> {
        require(isValidEvmAddress(address)) { "Not a valid EVM address" }
        val tokens = TokenRegistry.tokensFor(chain)
        val padded = address.removePrefix("0x").lowercase().padStart(64, '0')

        val batch: JsonArray = buildJsonArray {
            add(buildJsonObject {
                put("jsonrpc", "2.0"); put("id", 0); put("method", "eth_getBalance")
                put("params", buildJsonArray { add(JsonPrimitive(address)); add(JsonPrimitive("latest")) })
            })
            tokens.forEachIndexed { i, token ->
                add(buildJsonObject {
                    put("jsonrpc", "2.0"); put("id", i + 1); put("method", "eth_call")
                    put("params", buildJsonArray {
                        add(buildJsonObject { put("to", token.contract); put("data", "0x70a08231$padded") }) // balanceOf(address)
                        add(JsonPrimitive("latest"))
                    })
                })
            }
        }

        val body = try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(chain.rpcUrl)
                    .post(batch.toString().toRequestBody(jsonMedia))
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw EvmRpcException("${chain.displayName} RPC returned HTTP ${response.code}")
                    response.body?.string() ?: throw EvmRpcException("${chain.displayName} RPC returned an empty body")
                }
            }
        } catch (e: EvmRpcException) {
            throw e
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw EvmRpcException("${chain.displayName} is unreachable", e)
        }

        val results: Map<Int, JsonObject> = try {
            val parsed: JsonElement = json.parseToJsonElement(body)
            (parsed as? JsonArray)?.jsonArray
                ?.map { it.jsonObject }
                ?.associateBy { it["id"]!!.jsonPrimitive.int }
                ?: throw EvmRpcException("${chain.displayName} RPC returned an unexpected response")
        } catch (e: EvmRpcException) {
            throw e
        } catch (e: Exception) {
            throw EvmRpcException("${chain.displayName} RPC returned an unreadable response", e)
        }

        val balances = mutableListOf<RawBalance>()
        results[0]?.hexResult()?.let { wei ->
            if (wei.signum() > 0) balances += RawBalance(chain, null, BigDecimal(wei).movePointLeft(18))
        }
        tokens.forEachIndexed { i, token ->
            // A single token call failing (e.g. a contract absent on this chain) must not hide the rest.
            results[i + 1]?.hexResult()?.let { raw ->
                if (raw.signum() > 0) balances += RawBalance(chain, token, BigDecimal(raw).movePointLeft(token.decimals))
            }
        }
        return balances
    }

    /** `result` of a JSON-RPC reply as an unsigned integer, or null if the call errored / was empty. */
    private fun JsonObject.hexResult(): BigInteger? {
        val hex = this["result"]?.jsonPrimitive?.contentOrNull ?: return null
        val digits = hex.removePrefix("0x")
        return if (digits.isEmpty()) BigInteger.ZERO else runCatching { BigInteger(digits, 16) }.getOrNull()
    }
}
