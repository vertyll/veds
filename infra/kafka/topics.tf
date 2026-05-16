locals {
  business_topics = [
    "mail-requested",
    "mail-sent",
    "mail-failed",
    "saga-compensation-iam",
    "saga-compensation-mail",
  ]
}

resource "kafka_topic" "business" {
  for_each           = toset(local.business_topics)
  name               = each.value
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

resource "kafka_topic" "dlt" {
  for_each           = toset(local.business_topics)
  name               = "${each.value}-dlt"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}
