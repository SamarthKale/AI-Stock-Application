# Phase 6 — OCI Always Free provisioning, implementing the checklist in CLAUDE.md's Phase 6 plan
# §2c. NOT applied from this environment (no OCI credentials or `terraform`/`oci` CLI are
# available here) — written so you can run `terraform init && terraform plan` yourself against
# your own tenancy and review every resource before `terraform apply`. Every resource here is
# Always-Free-eligible; nothing here provisions a paid managed DB/cache/load balancer.

terraform {
  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0"
    }
  }
}

provider "oci" {
  tenancy_ocid     = var.tenancy_ocid
  user_ocid        = var.user_ocid
  fingerprint      = var.fingerprint
  private_key_path = var.oci_private_key_path
  region           = var.region
}

# --- Networking: one VCN, one public subnet, only 80/443/22 open (checklist step 3) ---

resource "oci_core_vcn" "this" {
  compartment_id = var.compartment_id
  cidr_blocks    = ["10.0.0.0/16"]
  display_name   = "ai-crypto-predictor-vcn"
}

resource "oci_core_internet_gateway" "this" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.this.id
  display_name   = "ai-crypto-predictor-igw"
}

resource "oci_core_default_route_table" "this" {
  manage_default_resource_id = oci_core_vcn.this.default_route_table_id
  route_rules {
    destination       = "0.0.0.0/0"
    network_entity_id = oci_core_internet_gateway.this.id
  }
}

resource "oci_core_security_list" "this" {
  compartment_id = var.compartment_id
  vcn_id         = oci_core_vcn.this.id
  display_name   = "ai-crypto-predictor-security-list"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }

  ingress_security_rules {
    source   = "0.0.0.0/0"
    protocol = "6" # TCP
    tcp_options { min = 80
      max = 80 }
  }
  ingress_security_rules {
    source   = "0.0.0.0/0"
    protocol = "6"
    tcp_options { min = 443
      max = 443 }
  }
  ingress_security_rules {
    # Restrict to your own IP in production — see CLAUDE.md's Phase 6 cost-safety checklist.
    source   = var.ssh_allowed_cidr
    protocol = "6"
    tcp_options { min = 22
      max = 22 }
  }
}

resource "oci_core_subnet" "this" {
  compartment_id             = var.compartment_id
  vcn_id                     = oci_core_vcn.this.id
  cidr_block                 = "10.0.1.0/24"
  display_name               = "ai-crypto-predictor-public-subnet"
  route_table_id             = oci_core_vcn.this.default_route_table_id
  security_list_ids          = [oci_core_security_list.this.id]
  prohibit_public_ip_on_vnic = false
}

# --- Compute: exactly 2 OCPU / 12 GB Ampere A1 — the entire Always Free A1 budget ---

resource "oci_core_instance" "app_vm" {
  compartment_id      = var.compartment_id
  availability_domain = var.availability_domain
  display_name        = "ai-crypto-predictor-vm"
  shape                = "VM.Standard.A1.Flex"

  shape_config {
    ocpus         = 2
    memory_in_gbs = 12
  }

  source_details {
    source_type = "image"
    source_id   = var.arm64_image_id # Ubuntu (or Oracle Linux) ARM64 Always-Free-eligible image OCID for your region
  }

  create_vnic_details {
    subnet_id        = oci_core_subnet.this.id
    assign_public_ip = false # a Reserved Public IP is attached separately below, not an ephemeral one
  }

  metadata = {
    ssh_authorized_keys = var.ssh_public_key
  }
}

# --- Reserved Public IPv4 (checklist step 5) — stays bound across reboots, unlike the ephemeral default ---

resource "oci_core_public_ip" "reserved" {
  compartment_id = var.compartment_id
  lifetime       = "RESERVED"
  display_name   = "ai-crypto-predictor-reserved-ip"
  private_ip_id  = data.oci_core_private_ips.vm_private_ip.private_ips[0].id
}

data "oci_core_private_ips" "vm_private_ip" {
  subnet_id  = oci_core_subnet.this.id
  ip_address = oci_core_instance.app_vm.private_ip
}

# --- Object Storage: model artifacts + Postgres backups (checklist step 7) ---

resource "oci_objectstorage_bucket" "model_artifacts" {
  compartment_id = var.compartment_id
  namespace      = var.object_storage_namespace
  name           = "ai-crypto-predictor-model-artifacts"
  access_type    = "NoPublicAccess"
}

resource "oci_objectstorage_bucket" "backups" {
  compartment_id = var.compartment_id
  namespace      = var.object_storage_namespace
  name           = "ai-crypto-predictor-backups"
  access_type    = "NoPublicAccess"
}

# Retention policy (§6 of the plan) enforced natively by Object Storage rather than by backup
# script logic — a dump left behind by a broken cron job still gets cleaned up on schedule.
resource "oci_objectstorage_object_lifecycle_policy" "backups_retention" {
  namespace = var.object_storage_namespace
  bucket    = oci_objectstorage_bucket.backups.name

  rules {
    name        = "expire-old-backups"
    action      = "DELETE"
    time_amount = 14
    time_unit   = "DAYS"
    is_enabled  = true
    target      = "objects"
  }
}

output "reserved_public_ip" {
  value = oci_core_public_ip.reserved.ip_address
}

output "sslip_hostname_hint" {
  value = "Replace dots with dashes in the reserved_public_ip output, then append .sslip.io — e.g. 203-0-113-10.sslip.io (see CLAUDE.md's Phase 6 plan §2b)."
}
