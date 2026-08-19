"""Pure feature-engineering functions over a (timestamp, price, volume) series.

Shared by both the XGBoost path (trained) and the LSTM/GRU stub (Phase 5 plan
section 6) so neither model type has to reimplement indicator math. Every
function takes/returns plain pandas Series/DataFrames — no I/O, no model
code, no CoinGecko-specific shapes — so this module has zero knowledge of
where the price data came from.

Indicators are hand-rolled (not pandas-ta) per the Phase 5 plan's licensing
note: pandas-ta's PyPI package metadata license field was empty at the time
of writing, and only ~8 indicators are actually needed here.
"""
from __future__ import annotations

import numpy as np
import pandas as pd


def sma(prices: pd.Series, window: int) -> pd.Series:
    return prices.rolling(window=window, min_periods=window).mean()


def ema(prices: pd.Series, span: int) -> pd.Series:
    return prices.ewm(span=span, adjust=False, min_periods=span).mean()


def rsi(prices: pd.Series, window: int = 14) -> pd.Series:
    delta = prices.diff()
    gain = delta.clip(lower=0.0)
    loss = -delta.clip(upper=0.0)
    avg_gain = gain.rolling(window=window, min_periods=window).mean()
    avg_loss = loss.rolling(window=window, min_periods=window).mean()
    rs = avg_gain / avg_loss.replace(0.0, np.nan)
    result = 100.0 - (100.0 / (1.0 + rs))
    # A zero average loss means every recent move was a gain -> RSI is 100,
    # not NaN (which the division above produces for a zero denominator).
    result = result.where(avg_loss != 0.0, 100.0)
    return result


def macd(prices: pd.Series, fast: int = 12, slow: int = 26, signal: int = 9) -> pd.DataFrame:
    ema_fast = ema(prices, fast)
    ema_slow = ema(prices, slow)
    macd_line = ema_fast - ema_slow
    signal_line = macd_line.ewm(span=signal, adjust=False, min_periods=signal).mean()
    histogram = macd_line - signal_line
    return pd.DataFrame({"macd_line": macd_line, "macd_signal": signal_line, "macd_hist": histogram})


def bollinger_bands(prices: pd.Series, window: int = 20, num_std: float = 2.0) -> pd.DataFrame:
    mid = sma(prices, window)
    std = prices.rolling(window=window, min_periods=window).std()
    upper = mid + num_std * std
    lower = mid - num_std * std
    bandwidth = (upper - lower) / mid.replace(0.0, np.nan)
    return pd.DataFrame({"bb_upper": upper, "bb_lower": lower, "bb_bandwidth": bandwidth})


def rate_of_change(prices: pd.Series, periods: int) -> pd.Series:
    return prices.pct_change(periods=periods, fill_method=None) * 100.0


def rolling_volatility(prices: pd.Series, window: int) -> pd.Series:
    returns = prices.pct_change(fill_method=None)
    return returns.rolling(window=window, min_periods=window).std() * 100.0


def volume_trend(volume: pd.Series, window: int = 14) -> pd.Series:
    avg_volume = volume.rolling(window=window, min_periods=window).mean()
    return volume / avg_volume.replace(0.0, np.nan)


def align_btc_prices(timestamps: pd.Series, btc_df: pd.DataFrame, tolerance_ms: int = 12 * 60 * 60 * 1000) -> pd.Series:
    """Aligns a target coin's timestamps to BTC's price series by *nearest* timestamp,
    not exact equality. CoinGecko's daily-history timestamps land on clean UTC
    midnight boundaries for every historical point, but the most recent ("live")
    point in each coin's series is stamped with the actual fetch time, which
    differs by tens of milliseconds between two coins fetched moments apart --
    an exact-match join silently drops exactly that row (verified empirically:
    365/366 timestamps matched exactly, the live point was the one miss). This
    is the row inference cares about most, so exact matching is not safe here.
    """
    btc_sorted = btc_df[["timestamp", "price"]].sort_values("timestamp")
    target = pd.DataFrame({"timestamp": timestamps.sort_values().values})
    merged = pd.merge_asof(
        target, btc_sorted, on="timestamp", direction="nearest", tolerance=tolerance_ms,
    )
    aligned = merged.set_index("timestamp")["price"]
    return timestamps.map(aligned)


def btc_relative_features(coin_prices: pd.Series, btc_prices: pd.Series, window: int = 30) -> pd.DataFrame:
    """Crypto-specific signal with no stock-market equivalent: altcoin price action is
    frequently BTC-driven, so a coin's return correlation/beta to BTC over a trailing
    window is informative even when the coin's own indicators look ambiguous. `btc_prices`
    must be aligned to the same timestamps as `coin_prices` by the caller.
    """
    coin_returns = coin_prices.pct_change(fill_method=None)
    btc_returns = btc_prices.pct_change(fill_method=None)
    btc_return_24h = btc_returns * 100.0
    rolling_corr = coin_returns.rolling(window=window, min_periods=window).corr(btc_returns)
    rolling_cov = coin_returns.rolling(window=window, min_periods=window).cov(btc_returns)
    btc_var = btc_returns.rolling(window=window, min_periods=window).var()
    rolling_beta = rolling_cov / btc_var.replace(0.0, np.nan)
    return pd.DataFrame({
        "btc_return_24h": btc_return_24h,
        "btc_corr_30d": rolling_corr,
        "btc_beta_30d": rolling_beta,
    })


# Longest lookback any feature below needs before its first non-NaN value —
# used both to size the training purge window (Phase 5 plan section 9) and to
# validate that an inference request has enough history (Phase 5 plan section 7).
#
# This is NOT simply the largest single window (30, for sma_30/btc_corr_30d/
# btc_beta_30d) -- macd_signal is an EMA-of-an-EMA: macd_line needs 26 rows
# (slow EMA's min_periods) before its own first valid value, and macd_signal
# then needs a further 9 valid macd_line values on top of that (26 + 9 - 1 =
# 34). Verified empirically via build_dataset.py's NaN check; do not lower
# this without re-running that check.
REQUIRED_WARMUP_DAYS = 40


def build_feature_frame(df: pd.DataFrame, btc_prices: pd.Series | None = None) -> pd.DataFrame:
    """df must have columns: timestamp, price, volume, sorted ascending by timestamp.
    Returns df with feature columns appended (rows before REQUIRED_WARMUP_DAYS worth of
    history will have NaN features and must be dropped by the caller before training).
    """
    out = df.copy()
    prices = out["price"]
    volume = out["volume"]

    out["sma_7"] = sma(prices, 7)
    out["sma_14"] = sma(prices, 14)
    out["sma_30"] = sma(prices, 30)
    out["ema_12"] = ema(prices, 12)
    out["ema_26"] = ema(prices, 26)
    out["rsi_14"] = rsi(prices, 14)

    macd_df = macd(prices)
    out = pd.concat([out, macd_df], axis=1)

    bb_df = bollinger_bands(prices)
    out = pd.concat([out, bb_df], axis=1)

    out["roc_1d"] = rate_of_change(prices, 1)
    out["roc_7d"] = rate_of_change(prices, 7)
    out["volatility_7d"] = rolling_volatility(prices, 7)
    out["volatility_14d"] = rolling_volatility(prices, 14)
    out["volume_trend_14d"] = volume_trend(volume, 14)

    if btc_prices is not None:
        btc_df = btc_relative_features(prices, btc_prices)
        out = pd.concat([out, btc_df], axis=1)

    return out


FEATURE_COLUMNS = [
    "sma_7", "sma_14", "sma_30", "ema_12", "ema_26", "rsi_14",
    "macd_line", "macd_signal", "macd_hist",
    "bb_upper", "bb_lower", "bb_bandwidth",
    "roc_1d", "roc_7d", "volatility_7d", "volatility_14d", "volume_trend_14d",
    "btc_return_24h", "btc_corr_30d", "btc_beta_30d",
]
