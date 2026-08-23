variable "tenancy_ocid" {
  description = "OCI tenancy OCID"
  type        = string
}

variable "user_ocid" {
  description = "OCI user OCID (the account whose API key is used below)"
  type        = string
}

variable "fingerprint" {
  description = "Fingerprint of the API signing key added to the user above"
  type        = string
}

variable "oci_private_key_path" {
  description = "Path to the local private key file for the API signing key (never committed)"
  type        = string
}

variable "region" {
  description = "OCI region, e.g. us-ashburn-1"
  type        = string
}

variable "compartment_id" {
  description = "Compartment OCID to provision into (root compartment is fine for a project this size)"
  type        = string
}

variable "availability_domain" {
  description = "Availability domain name for the compute instance, e.g. \"Uocm:US-ASHBURN-AD-1\""
  type        = string
}

variable "arm64_image_id" {
  description = "OCID of an Always-Free-eligible ARM64 (Ubuntu or Oracle Linux) image for your region — look this up in the OCI console's Compute > Images page, or via `oci compute image list --shape VM.Standard.A1.Flex`"
  type        = string
}

variable "ssh_public_key" {
  description = "Public half of the SSH key you'll use to reach the VM directly (separate from the GitHub Actions deploy key in §4)"
  type        = string
}

variable "ssh_allowed_cidr" {
  description = "CIDR allowed to reach port 22 — restrict to your own IP/32 in production, per the Phase 6 cost-safety checklist"
  type        = string
  default     = "0.0.0.0/0"
}

variable "object_storage_namespace" {
  description = "Your tenancy's Object Storage namespace (OCI console: Object Storage > Buckets shows it, or `oci os ns get`)"
  type        = string
}
