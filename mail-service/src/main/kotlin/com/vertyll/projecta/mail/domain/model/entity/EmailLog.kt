package com.vertyll.projecta.mail.domain.model.entity

import com.vertyll.projecta.mail.domain.model.enums.EmailStatus
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
class EmailLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @Column(nullable = false)
    val recipient: String,
    @Column(nullable = false)
    val subject: String,
    @Column(nullable = false)
    val templateName: String,
    @Column(nullable = true, length = 4000)
    val variables: String? = null,
    @Column(nullable = true)
    val replyTo: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: EmailStatus = EmailStatus.PENDING,
    @Column(nullable = true, length = 1000)
    var errorMessage: String? = null,
    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),
    @Column(nullable = true)
    var sentAt: Instant? = null,
) {
    constructor() : this(
        id = null,
        recipient = "",
        subject = "",
        templateName = "",
        variables = null,
        replyTo = null,
        status = EmailStatus.PENDING,
        errorMessage = null,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        sentAt = null,
    )
}
