package com.vertyll.veds.mail.infrastructure.persistence.entity

import com.vertyll.veds.mail.domain.model.EmailStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "email_log")
class EmailLogJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false)
    var recipient: String,
    @Column(nullable = false)
    var subject: String,
    @Column(nullable = false)
    var templateName: String,
    @Column(nullable = true, length = 4000)
    var variables: String? = null,
    @Column(nullable = true)
    var replyTo: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EmailStatus = EmailStatus.PENDING,
    @Column(nullable = true, length = 1000)
    var errorMessage: String? = null,
    @Column(nullable = false)
    var createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(nullable = true)
    var sentAt: Instant? = null,
)
