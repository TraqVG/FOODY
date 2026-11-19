package com.foody.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FoodyNotifier.createChannel(applicationContext)
        setContent {
            FoodyTheme {
                val state: FoodyStore = viewModel()
                FoodyApp(state)
            }
        }
    }
}

@Composable
fun FoodyApp(state: FoodyStore) {
    val context=LocalContext.current
    val requestNotificationPermission=rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* A denied permission does not block ordering. */ }
    LaunchedEffect(state.role) {
        if(state.role!=null && Build.VERSION.SDK_INT>=33 &&
           ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
           ) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    if(!state.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    var editId by remember { mutableStateOf<Int?>(null) }
    val destination=state.route
    val primaryRoots=setOf("tables","history","kitchen","reports","manage")
    val backTo=when(destination) {
        "table"->"tables"
        "menu"->"table"
        "cart"->"menu"
        "checkout"->"table"
        "editor"->"manage"
        "settings"->when(state.role) {
            FoodyRole.STAFF->"tables"
            FoodyRole.KITCHEN->"kitchen"
            FoodyRole.OWNER->"reports"
            null->"login"
        }
        else->null
    }
    BackHandler(enabled=destination !in primaryRoots && destination!="login") {
        state.go(backTo ?: "tables")
    }
    if(state.role==null) {
        LoginScreen(state)
        return
    }
    val snackbarState=remember{SnackbarHostState()}
    LaunchedEffect(state.message) {
        if(state.role!=null && state.message.isNotBlank()) {
            snackbarState.showSnackbar(state.message)
        }
    }
    Scaffold(
        containerColor=FoodyBackground,
        snackbarHost={SnackbarHost(snackbarState)},
        topBar={
            FoodyTopBar(
                title=when(destination){
                    "tables"->"โต๊ะอาหาร"
                    "table"->"รายละเอียดโต๊ะ"
                    "menu"->"เลือกเมนู"
                    "cart"->"รายการสั่ง"
                    "history"->"ประวัติออเดอร์"
                    "kitchen"->"คิวครัว"
                    "reports"->"รายงาน"
                    "manage"->"จัดการเมนู"
                    "editor"->if(editId==null)"เพิ่มเมนู" else "แก้ไขเมนู"
                    "checkout"->"เช็กบิล"
                    "settings"->"ตั้งค่า"
                    else->"Foody"
                },
                onBack=backTo?.let {{state.go(it)}},
                onProfile=if(destination=="settings")null else {{state.go("settings")}}
            )
        },
        bottomBar={
            when(destination){
                "tables","history"->FoodyBottomNav(
                    labels=listOf("โต๊ะ" to "tables","ประวัติ" to "history"),
                    selected=destination,onNavigate=state::go
                )
                "reports","manage"->FoodyBottomNav(
                    labels=listOf("รายงาน" to "reports","เมนู" to "manage"),
                    selected=destination,onNavigate=state::go
                )
                "table"->if(state.table(state.currentTable).guests==0)
                    BottomAction("เปิดโต๊ะ",onClick={state.openTable()})
                else TableActions(state)
                "menu"->MenuCartDock(state)
                "cart"->BottomAction(
                    "ส่งออเดอร์เข้าครัว",
                    enabled=state.cart.isNotEmpty()&&!state.busy,
                    onClick={state.sendOrder()}
                )
                "checkout"->BottomAction(
                    "ยืนยันรับเงินและปิดโต๊ะ",
                    enabled=state.canCheckout(state.currentTable)&&!state.busy,
                    onClick={state.checkout()}
                )
                "settings"->BottomAction("ออกจากระบบ",danger=true,onClick={state.logout()})
            }
        }
    ){padding->
        Box(Modifier.fillMaxSize().padding(padding)){
            when(destination){
                "tables"->TablesScreen(state)
                "table"->if(state.table(state.currentTable).guests==0)
                    EmptyTableScreen(state) else OccupiedTableScreen(state)
                "menu"->MenuPickerScreen(state)
                "cart"->CartScreen(state)
                "history"->HistoryScreen(state)
                "kitchen"->KitchenScreen(state)
                "reports"->ReportsScreen(state)
                "manage"->ManageScreen(state,onEdit={id->editId=id;state.go("editor")})
                "editor"->MenuEditorScreen(state,editId,onSaved={state.go("manage")})
                "checkout"->CheckoutScreen(state)
                "settings"->SettingsScreen(state)
            }
            if(state.busy){
                LinearProgressIndicator(
                    modifier=Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color=FoodyOrange
                )
            }
        }
    }
}
