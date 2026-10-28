package com.connectpro.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.connectpro.data.contacts.EmergencyContactRepository
import com.connectpro.data.contacts.EmergencyContactResponse
import com.connectpro.data.network.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

@Composable
fun TrustedContactsContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember(context) {
        EmergencyContactRepository(
            ApiClient(context.applicationContext)
        )
    }

    var contacts by remember {
        mutableStateOf<List<EmergencyContactResponse>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    var showEditor by remember { mutableStateOf(false) }
    var editingContact by remember {
        mutableStateOf<EmergencyContactResponse?>(null)
    }
    var contactToDelete by remember {
        mutableStateOf<EmergencyContactResponse?>(null)
    }

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }

    fun readableError(error: Throwable): String {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> "Your session has expired. Please sign in again."
                403 -> "You are not allowed to perform this action."
                400 -> "The server rejected the request. Check the contact details."
                else -> "Request failed (HTTP ${error.code()}). Please try again."
            }

            is IOException ->
                "Unable to reach the server. Check your connection and try again."

            else ->
                "Something went wrong. Please try again."
        }
    }

    fun loadContacts() {
        scope.launch {
            isLoading = true
            errorMessage = null

            try {
                contacts = repository.getAll()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = readableError(error)
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(repository) {
        loadContacts()
    }

    fun openAddForm() {
        editingContact = null
        name = ""
        phoneNumber = ""
        relationship = ""
        formError = null
        showEditor = true
    }

    fun openEditForm(contact: EmergencyContactResponse) {
        editingContact = contact
        name = contact.name
        phoneNumber = contact.phoneNumber
        relationship = contact.relationship
        formError = null
        showEditor = true
    }

    fun saveContact() {
        if (name.isBlank() ||
            phoneNumber.isBlank() ||
            relationship.isBlank()
        ) {
            formError = "Please complete all three fields."
            return
        }

        if (name.trim().length > 100 ||
            phoneNumber.trim().length > 30 ||
            relationship.trim().length > 50
        ) {
            formError = "One or more fields exceed the allowed length."
            return
        }

        scope.launch {
            isSaving = true
            formError = null
            errorMessage = null

            try {
                val existing = editingContact

                if (existing == null) {
                    repository.create(
                        name = name,
                        phoneNumber = phoneNumber,
                        relationship = relationship
                    )
                } else {
                    repository.update(
                        contactId = existing.id,
                        name = name,
                        phoneNumber = phoneNumber,
                        relationship = relationship
                    )
                }

                showEditor = false
                loadContacts()
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                formError = readableError(error)
            } finally {
                isSaving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Trusted contacts",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Manage the people you trust as part of your safety network.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { openAddForm() },
                enabled = !isSaving
            ) {
                Text("Add contact")
            }

            TextButton(
                onClick = { loadContacts() },
                enabled = !isLoading
            ) {
                Text("Refresh")
            }
        }

        errorMessage?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator()
        } else if (contacts.isEmpty() && errorMessage == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No trusted contacts yet",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Add someone you trust so their details are available in your safety network.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            contacts.forEach { contact ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Text(
                            text = "Relationship: ${contact.relationship}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = if (contact.status == "ACTIVE") {
                                "Status: Active"
                            } else {
                                "Status: Inactive"
                            },
                            color = if (contact.status == "ACTIVE") {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelLarge
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { openEditForm(contact) }
                            ) {
                                Text("Edit")
                            }

                            TextButton(
                                onClick = {
                                    scope.launch {
                                        errorMessage = null

                                        try {
                                            if (contact.status == "ACTIVE") {
                                                repository.deactivate(contact.id)
                                            } else {
                                                repository.activate(contact.id)
                                            }
                                            loadContacts()
                                        } catch (error: kotlinx.coroutines.CancellationException) {
                                            throw error
                                        } catch (error: Exception) {
                                            errorMessage = readableError(error)
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    if (contact.status == "ACTIVE") {
                                        "Deactivate"
                                    } else {
                                        "Activate"
                                    }
                                )
                            }

                            TextButton(
                                onClick = {
                                    contactToDelete = contact
                                }
                            ) {
                                Text(
                                    text = "Delete",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) showEditor = false
            },
            title = {
                Text(
                    if (editingContact == null) {
                        "Add trusted contact"
                    } else {
                        "Edit trusted contact"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full name") },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone number") },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("Relationship") },
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    )

                    formError?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { saveContact() },
                    enabled = !isSaving
                ) {
                    Text(if (isSaving) "Saving…" else "Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditor = false },
                    enabled = !isSaving
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Delete contact?") },
            text = {
                Text("Remove ${contact.name} from your trusted contacts?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            errorMessage = null

                            try {
                                repository.delete(contact.id)
                                contactToDelete = null
                                loadContacts()
                            } catch (error: kotlinx.coroutines.CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                errorMessage = readableError(error)
                                contactToDelete = null
                            }
                        }
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { contactToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
