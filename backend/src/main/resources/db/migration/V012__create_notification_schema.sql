CREATE TABLE notification.notification_record (
    notification_id      UUID PRIMARY KEY,
    order_id              UUID NOT NULL,
    channel                VARCHAR(20) NOT NULL,
    recipient               VARCHAR(500) NOT NULL,
    type                     VARCHAR(30) NOT NULL,
    status                   VARCHAR(20) NOT NULL,
    provider_message_id      VARCHAR(200),
    failure_reason            VARCHAR(1000),
    created_at                TIMESTAMPTZ NOT NULL,
    sent_at                   TIMESTAMPTZ
);

CREATE INDEX idx_notification_record_order_id ON notification.notification_record (order_id);
