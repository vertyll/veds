terraform {
  required_version = ">= 1.6.0"

  required_providers {
    kafka = {
      source  = "Mongey/kafka"
      version = ">= 0.6.0"
    }
  }
}

provider "kafka" {
  bootstrap_servers = var.bootstrap_servers
}

