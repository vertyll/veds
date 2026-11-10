-- Serves the outbox claim's ordering guard, which looks for an older unsent row with the same
-- topic and key before it dispatches a row. Partial, so it holds only the rows still to be sent
-- and stays small however many completed rows the table keeps.
--
-- Written by the shared messaging module, not by this service's domain: a clone copies it
-- verbatim. See docs/mechanisms/transactional-outbox.md.

CREATE INDEX idx_kafka_outbox_topic_key_id_unsent ON kafka_outbox (topic, key, id)
    WHERE status IN ('PENDING', 'PROCESSING');
