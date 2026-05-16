variable "bootstrap_servers" {
  type        = string
  description = "Kafka bootstrap servers, e.g., localhost:29092"
  default     = "localhost:29092"
}

variable "partitions" {
  type        = number
  description = "Default partition count for topics"
  default     = 1
}

variable "replication_factor" {
  type        = number
  description = "Default replication factor for topics"
  default     = 1
}

