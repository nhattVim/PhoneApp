package com.example.phoneapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phoneapp.ui.theme.PhoneAppTheme
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneAppTheme {
                ContactListScreen()
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    PhoneAppTheme {
        ContactListScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar() {
    TopAppBar(
        title = { Text("Danh Bạ Điện Thoại") },
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Quay lại"
                )
            }
        },
        actions = {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Tìm kiếm"
                )
            }
        },
        modifier = Modifier.background(Color.Blue)
    )
}

data class Contact(val id: Int, val name: String, val phoneNumber: String)

val sampleContacts = mutableStateListOf(
    Contact(1, "Nguyen Van A", "0123456789"),
    Contact(2, "Le Thi B", "0987654321"),
    Contact(3, "Tran Van C", "0121987654")
)

@Composable
fun ContactListScreen() {
    val context = LocalContext.current
    var name by remember { mutableStateOf(TextFieldValue()) }
    var phoneNumber by remember { mutableStateOf(TextFieldValue()) }
    var searchQuery by remember { mutableStateOf(TextFieldValue()) }

    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }
    var deletingContact by remember { mutableStateOf<Contact?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopBar()

        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Tìm kiếm (Tên / SĐT)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        val filteredContacts = sampleContacts.filter {
            it.name.contains(searchQuery.text, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery.text)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredContacts.size) { index ->
                val contact = filteredContacts[index]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { selectedContact = contact },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = contact.name, fontSize = 18.sp)
                        Text(text = contact.phoneNumber, fontSize = 16.sp)
                    }

                    IconButton(onClick = {
                        editingContact = contact
                        name = TextFieldValue(contact.name)
                        phoneNumber = TextFieldValue(contact.phoneNumber)
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa")
                    }

                    Button(onClick = { deletingContact = contact }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa")
                        Text("Xóa", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên liên hệ") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Số điện thoại") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        )

        Button(
            onClick = {
                if (name.text.isNotBlank() && phoneNumber.text.isNotBlank()) {
                    val phone = phoneNumber.text.trim()

                    if (phone.matches(Regex("^\\d{10}$"))) {
                        if (editingContact == null) {
                            val newContact = Contact(
                                id = (sampleContacts.maxOfOrNull { it.id } ?: 0) + 1,
                                name = name.text,
                                phoneNumber = phone
                            )
                            sampleContacts.add(newContact)
                        } else {
                            val index = sampleContacts.indexOfFirst { it.id == editingContact!!.id }
                            if (index != -1) {
                                sampleContacts[index] = editingContact!!.copy(
                                    name = name.text,
                                    phoneNumber = phone
                                )
                            }
                            editingContact = null
                        }

                        name = TextFieldValue("")
                        phoneNumber = TextFieldValue("")
                    } else {
                        Toast.makeText(
                            context,
                            "Số điện thoại phải gồm 10 chữ số!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (editingContact == null) "Thêm liên hệ" else "Cập nhật liên hệ")
        }
    }

    if (selectedContact != null) {
        AlertDialog(
            onDismissRequest = { selectedContact = null },
            confirmButton = {
                TextButton(onClick = { selectedContact = null }) {
                    Text("Đóng")
                }
                TextButton(onClick = {
                    makeCall(context, phoneNumber = selectedContact?.phoneNumber.toString())
                }) {
                    Text("Gọi")
                }
            },
            title = { Text("Chi tiết Liên Hệ") },
            text = {
                Column {
                    Text("Tên: ${selectedContact?.name}")
                    Text("Số điện thoại: ${selectedContact?.phoneNumber}")
                }
            }
        )
    }

    if (deletingContact != null) {
        AlertDialog(
            onDismissRequest = { deletingContact = null },
            confirmButton = {
                TextButton(onClick = {
                    sampleContacts.remove(deletingContact)
                    deletingContact = null
                }) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingContact = null }) {
                    Text("Hủy")
                }
            },
            title = { Text("Xác nhận xóa") },
            text = { Text("Bạn có chắc muốn xóa ${deletingContact?.name}?") }
        )
    }
}

fun makeCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = "tel:$phoneNumber".toUri()
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
        != PackageManager.PERMISSION_GRANTED
    ) {
        if (context is ComponentActivity) {
            ActivityCompat.requestPermissions(
                context,
                arrayOf(Manifest.permission.CALL_PHONE),
                100
            )
        } else {
            Toast.makeText(context, "Không có quyền gọi điện", Toast.LENGTH_SHORT).show()
        }
    } else {
        context.startActivity(intent)
    }
}
