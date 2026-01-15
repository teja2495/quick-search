
package com.tk.quicksearch.search.contacts.models

/**
 * Represents a customizable action on the contact card.
 * Can be assigned to primary (call) or secondary (message) positions.
 */
sealed class ContactCardAction {
    abstract val phoneNumber: String
    
    data class Phone(override val phoneNumber: String) : ContactCardAction()
    data class Sms(override val phoneNumber: String) : ContactCardAction()
    data class WhatsAppCall(override val phoneNumber: String) : ContactCardAction()
    data class WhatsAppMessage(override val phoneNumber: String) : ContactCardAction()
    data class WhatsAppVideoCall(override val phoneNumber: String) : ContactCardAction()
    data class TelegramMessage(override val phoneNumber: String) : ContactCardAction()
    data class TelegramCall(override val phoneNumber: String) : ContactCardAction()
    data class TelegramVideoCall(override val phoneNumber: String) : ContactCardAction()
    data class GoogleMeet(override val phoneNumber: String) : ContactCardAction()
    
    /**
     * Serializes the action to a string format: "TYPE:PHONE_NUMBER"
     */
    fun toSerializedString(): String {
        val type = when (this) {
            is Phone -> TYPE_PHONE
            is Sms -> TYPE_SMS
            is WhatsAppCall -> TYPE_WHATSAPP_CALL
            is WhatsAppMessage -> TYPE_WHATSAPP_MESSAGE
            is WhatsAppVideoCall -> TYPE_WHATSAPP_VIDEO_CALL
            is TelegramMessage -> TYPE_TELEGRAM_MESSAGE
            is TelegramCall -> TYPE_TELEGRAM_CALL
            is TelegramVideoCall -> TYPE_TELEGRAM_VIDEO_CALL
            is GoogleMeet -> TYPE_GOOGLE_MEET
        }
        return "$type:$phoneNumber"
    }
    
    companion object {
        private const val TYPE_PHONE = "PHONE"
        private const val TYPE_SMS = "SMS"
        private const val TYPE_WHATSAPP_CALL = "WHATSAPP_CALL"
        private const val TYPE_WHATSAPP_MESSAGE = "WHATSAPP_MESSAGE"
        private const val TYPE_WHATSAPP_VIDEO_CALL = "WHATSAPP_VIDEO_CALL"
        private const val TYPE_TELEGRAM_MESSAGE = "TELEGRAM_MESSAGE"
        private const val TYPE_TELEGRAM_CALL = "TELEGRAM_CALL"
        private const val TYPE_TELEGRAM_VIDEO_CALL = "TELEGRAM_VIDEO_CALL"
        private const val TYPE_GOOGLE_MEET = "GOOGLE_MEET"
        
        /**
         * Deserializes a string back to a ContactCardAction
         */
        fun fromSerializedString(value: String): ContactCardAction? {
            val parts = value.split(":", limit = 2)
            if (parts.size != 2) return null
            
            val type = parts[0]
            val phoneNumber = parts[1]
            
            return when (type) {
                TYPE_PHONE -> Phone(phoneNumber)
                TYPE_SMS -> Sms(phoneNumber)
                TYPE_WHATSAPP_CALL -> WhatsAppCall(phoneNumber)
                TYPE_WHATSAPP_MESSAGE -> WhatsAppMessage(phoneNumber)
                TYPE_WHATSAPP_VIDEO_CALL -> WhatsAppVideoCall(phoneNumber)
                TYPE_TELEGRAM_MESSAGE -> TelegramMessage(phoneNumber)
                TYPE_TELEGRAM_CALL -> TelegramCall(phoneNumber)
                TYPE_TELEGRAM_VIDEO_CALL -> TelegramVideoCall(phoneNumber)
                TYPE_GOOGLE_MEET -> GoogleMeet(phoneNumber)
                else -> null
            }
        }
    }
}
