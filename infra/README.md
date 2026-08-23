# Phase 6 — OCI Terraform (not applied)

Implements the provisioning checklist in `CLAUDE.md`'s Phase 6 plan §2c: VCN + public subnet +
security list (80/443/22 only), the 2 OCPU / 12 GB Ampere A1 VM, a Reserved Public IPv4, and two
private Object Storage buckets (model artifacts, backups).

**Not run from this environment** — no OCI credentials or `terraform`/`oci` CLI were available
where this was written. Review every resource yourself before applying; this is infrastructure
that costs real money if misconfigured outside the Always Free allowance (see the cost-safety
checklist in the plan file).

## Before you run this

1. Confirm your tenancy is genuine Always Free (not upgraded to Pay-As-You-Go) — OCI console,
   Billing/Cost Management. Set a budget alarm at a near-₹0 threshold first.
2. Generate an OCI API signing key pair (OCI console: your user → API Keys → Add API Key) and
   note the fingerprint it gives you.
3. Look up your compartment OCID, availability domain name, an Always-Free-eligible ARM64 image
   OCID for your region, and your Object Storage namespace (all in the OCI console).
4. Generate (or reuse) an SSH key pair for direct VM access — separate from the GitHub Actions
   deploy key described in `CLAUDE.md`'s Phase 6 plan §5/§2c.

## Running it

```
cd infra
cp terraform.tfvars.example terraform.tfvars   # fill in real values, never commit this file
terraform init
terraform plan   # review every resource before applying anything
terraform apply
```

`terraform.tfvars` is gitignored (see the repo's `.gitignore`) — it holds only OCIDs and a public
key, no secrets in the traditional sense, but treat it as sensitive anyway since it identifies
your exact tenancy/compartment.

## After applying

- Note the `reserved_public_ip` output and derive the sslip.io hostname per `sslip_hostname_hint`.
- Continue with the rest of `CLAUDE.md`'s Phase 6 plan §2c checklist steps 8-12 (Customer Secret
  Keys for Object Storage, the GitHub Actions SSH deploy key, installing Docker on the VM, placing
  `.env`/the Firebase service-account JSON, first `docker compose up -d` + Certbot) — none of
  those are Terraform resources; they're manual/scripted steps against the VM itself.
