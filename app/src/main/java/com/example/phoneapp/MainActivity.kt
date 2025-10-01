package com.example.phoneapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.phoneapp.ui.theme.PhoneAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneAppTheme { PhoneAppNavHost() }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Contacts : Screen("contacts", "Danh bạ", Icons.Default.Phone)
    object Favorites : Screen("favorites", "Yêu thích", Icons.Default.Favorite)
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Contacts,
    Screen.Favorites,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAppNavHost() {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            val currentScreen = bottomNavItems.find { it.route == currentDestination?.route }
            TopBar(title = currentScreen?.label ?: "Ứng dụng")
        },
        bottomBar = {
            NavigationBar {
                val currentDestination =
                    navController.currentBackStackEntryAsState().value?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination.isRouteActive(screen.route),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Contacts.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Contacts.route) { ContactsContent() }
            composable(Screen.Favorites.route) { PlaceholderScreen("Tab Yêu thích đang trống") }
            composable(Screen.Settings.route) { PlaceholderScreen("Tab Cài đặt — tuỳ chỉnh ở đây") }
        }
    }
}

fun NavDestination?.isRouteActive(route: String): Boolean {
    return this?.route == route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(title: String) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = "Tìm kiếm")
            }
        },
        modifier = Modifier.background(Color(0xFF1565C0))
    )
}

data class Contact(val id: Int, val name: String, val phoneNumber: String)

val sampleContacts = mutableStateListOf(
    Contact(1, "Nguyen Van A", "0123456789"),
    Contact(2, "Le Thi B", "0987654321"),
    Contact(3, "Tran Van C", "0121987654")
)

@Composable
fun ContactsContent() {
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

                    IconButton(onClick = { deletingContact = contact }) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa")
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

@Composable
fun PlaceholderScreen(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text)
    }
}

fun makeCall(context: Context, phoneNumber: String) {
    val intent = Intent(Intent.ACTION_CALL).apply {
        data = "tel:$phoneNumber".toUri()
    }

    if (context.checkSelfPermission(Manifest.permission.CALL_PHONE)
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
