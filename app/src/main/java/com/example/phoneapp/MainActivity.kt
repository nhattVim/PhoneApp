package com.example.phoneapp

import android.Manifest
import android.app.Application
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.phoneapp.data.AppDatabase
import com.example.phoneapp.data.Contact
import com.example.phoneapp.ui.theme.PhoneAppTheme

class PhoneApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}

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
    object Numboards : Screen("numboards", "Ban phim", Icons.Default.Phone)
    object Contacts : Screen("contacts", "Danh bạ", Icons.Default.Person)
    object Favorites : Screen("favorites", "Yêu thích", Icons.Default.Favorite)
    object Settings : Screen("settings", "Cài đặt", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Numboards,
    Screen.Contacts,
    Screen.Favorites,
    Screen.Settings,
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
            startDestination = Screen.Numboards.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Contacts.route) {
                // Khởi tạo ViewModel ở đây
                val context = LocalContext.current
                val viewModel: ContactViewModel = viewModel(
                    factory = ContactViewModelFactory((context.applicationContext as PhoneApplication).database.contactDao())
                )
                ContactsScreen(viewModel)
            }
            composable(Screen.Favorites.route) { PlaceholderScreen("Tuyet Khung") }
            composable(Screen.Settings.route) { PlaceholderScreen("Thi Khung") }
            composable(Screen.Numboards.route) { NumboardsScreen() }
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


@Composable
fun ContactsScreen(viewModel: ContactViewModel) {
    val context = LocalContext.current
    val allContacts by viewModel.allContacts.collectAsState()

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
                .padding(bottom = 8.dp)
        )

        val filteredContacts = allContacts.filter {
            it.name.contains(searchQuery.text, ignoreCase = true) ||
                    it.phoneNumber.contains(searchQuery.text)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredContacts) { contact ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { selectedContact = contact },
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = contact.name, fontSize = 18.sp)
                        Text(text = contact.phoneNumber, fontSize = 16.sp, color = Color.Gray)
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
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Số điện thoại") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Button(
            onClick = {
                val contactName = name.text.trim()
                val contactPhone = phoneNumber.text.trim()
                if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                    if (contactPhone.matches(Regex("^\\d{10}$"))) {
                        if (editingContact == null) {
                            viewModel.addContact(contactName, contactPhone)
                        } else {
                            val updatedContact = editingContact!!.copy(
                                name = contactName,
                                phoneNumber = contactPhone
                            )
                            viewModel.updateContact(updatedContact)
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text(if (editingContact == null) "Thêm liên hệ" else "Cập nhật liên hệ")
        }
    }

    // Dialog chi tiết
    if (selectedContact != null) {
        AlertDialog(
            onDismissRequest = { selectedContact = null },
            confirmButton = {
                TextButton(onClick = {
                    makeCall(context, selectedContact!!.phoneNumber)
                    selectedContact = null
                }) {
                    Text("Gọi")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedContact = null }) {
                    Text("Đóng")
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

    // Dialog xóa
    if (deletingContact != null) {
        AlertDialog(
            onDismissRequest = { deletingContact = null },
            confirmButton = {
                TextButton(onClick = {
                    // XÓA
                    viewModel.deleteContact(deletingContact!!)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumboardsScreen() {
    val context = LocalContext.current
    var phoneNumber by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = phoneNumber.ifEmpty { "Nhập số để gọi" },
            fontSize = 32.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            color = if (phoneNumber.isEmpty()) Color.Gray else Color.Black

        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("*", "0", "#")
            )

            rows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { number ->
                        Button(
                            onClick = { phoneNumber += number },
                            modifier = Modifier
                                .size(80.dp),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                text = number,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank()) {
                        makeCall(context, phoneNumber)
                    } else {
                        Toast.makeText(
                            context,
                            "Nhập số điện thoại trước khi gọi",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Gọi",
                    tint = Color.White
                )
            }

            Button(
                onClick = {
                    if (phoneNumber.isNotEmpty()) {
                        phoneNumber = phoneNumber.dropLast(1)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                modifier = Modifier
                    .size(80.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = Color.White
                )
            }
        }
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
