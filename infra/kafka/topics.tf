resource "kafka_topic" "mail_requested" {
  name               = "mail-requested"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

resource "kafka_topic" "mail_sent" {
  name               = "mail-sent"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

resource "kafka_topic" "mail_failed" {
  name               = "mail-failed"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

resource "kafka_topic" "saga_compensation_iam" {
  name               = "saga-compensation-iam"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}

resource "kafka_topic" "saga_compensation_mail" {
  name               = "saga-compensation-mail"
  partitions         = var.partitions
  replication_factor = var.replication_factor
}
