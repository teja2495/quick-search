# Add a contact-button action

Define the action in `models/ContactButtonAction.kt`. Keep existing enum names stable because
preferences store them. Supply the label, icon, provider, method matcher, and card-action factory
as applicable. The same entry drives availability, number matching, Settings, and the button
icon. The generic custom-contact action preserves the MIME type, package name, and data row.

For a new messaging app, declare its package in `ContactActionProvider`, add its saved
`MessagingApp` choice, and link the message action. Startup and Settings use the provider set
automatically. Implement provider-specific contact data and intent handling in the owning feature.
Give the action a launch callback if it needs a direct launch or a provider-specific fallback.
Run the contact definition tests and manually check message, voice, video, and fallback paths
supported by that app.
