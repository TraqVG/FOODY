package com.foody.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MainActivity:ComponentActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        setContent { FoodyTheme { FoodyApp() } }
    }
}

@Composable
fun FoodyApp(vm:FoodyViewModel=viewModel()){
    val context=LocalContext.current
    val notifier=remember { FoodyNotifier(context) }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){}
    var editFood by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(vm.role){
        if(vm.role!=null && Build.VERSION.SDK_INT>=33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LaunchedEffect(vm.role){
        if(vm.role!=null) {
            while(isActive) {
                vm.refresh(silent=true)
                vm.fetchEvents().forEach { e -> notifier.notify(e) }
                delay(5_000)
            }
        }
    }
    BackHandler(enabled=vm.role!=null && vm.route !in listOf("tables","history","kitchen","reports","manage")) { vm.back() }
    if(vm.role==null) {
        LoginScreen(vm)
        return
    }
    val screen=vm.route
    val inner=screen in listOf("table","menu","cart","checkout","editor","settings")
    Scaffold(
        containerColor=Canvas,
        topBar={ FoodyHeader(
            title=when(screen){
                "tables"->"โต๊ะอาหาร";"table"->"รายละเอียดโต๊ะ";"menu"->"เลือกเมนู";"cart"->"รายการสั่ง"
                "checkout"->"เช็กบิล";"history"->"ประวัติออเดอร์";"kitchen"->"คิวครัว"
                "reports"->"รายงาน";"manage"->"จัดการเมนู";"editor"->if(editFood==null)"เพิ่มเมนู" else "แก้ไขเมนู"
                "settings"->"ตั้งค่า";else->"Foody"
            },
            onBack=if(inner)vm::back else null,
            onProfile=if(screen!="settings"){{vm.goto("settings")}} else null
        )},
        bottomBar={
            when(screen){
                "tables","history" -> FoodyNavigation(listOf("โต๊ะ" to "tables","ประวัติ" to "history"),screen,vm::goto)
                "reports","manage" -> FoodyNavigation(listOf("รายงาน" to "reports","เมนู" to "manage"),screen,vm::goto)
                "table" -> if(vm.table(vm.currentTable).guests==0)
                    BottomButton("เปิดโต๊ะ",enabled=!vm.busy,onClick=vm::openTable)
                else OccupiedBottom(vm)
                "menu" -> MenuBottom(vm)
                "cart" -> BottomButton("ส่งออเดอร์เข้าครัว",enabled=vm.cart.isNotEmpty()&&!vm.busy,onClick=vm::sendOrder)
                "checkout" -> BottomButton("ยืนยันรับเงินและปิดโต๊ะ",enabled=vm.canCheckout(vm.currentTable)&&!vm.busy,onClick=vm::checkout)
                "settings" -> BottomButton("ออกจากระบบ",danger=true,onClick=vm::logout)
            }
        }
    ){innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if(vm.offline) Surface(color=PaleOrange) {
                Text("ออฟไลน์ • แสดงข้อมูลล่าสุดที่บันทึกไว้",modifier=Modifier.padding(9.dp),color=OrangeDark)
            }
            if(vm.error.isNotBlank()) {
                Surface(color=PaleOrange) {
                    TextButton(onClick=vm::clearError,modifier=Modifier.fillMaxWidth()){
                        Text(vm.error+"  ×",color=OrangeDark)
                    }
                }
            }
            Box(Modifier.weight(1f)){
                when(screen){
                    "tables" -> TablesScreen(vm)
                    "table" -> if(vm.table(vm.currentTable).guests==0) GuestCountScreen(vm) else TableDetailScreen(vm)
                    "menu" -> MenuScreen(vm)
                    "cart" -> CartScreen(vm)
                    "history" -> HistoryScreen(vm)
                    "checkout" -> CheckoutScreen(vm)
                    "kitchen" -> KitchenScreen(vm)
                    "reports" -> ReportsScreen(vm)
                    "manage" -> ManageMenuScreen(vm){ id->editFood=id;vm.goto("editor") }
                    "editor" -> MenuEditorScreen(vm,editFood)
                    "settings" -> SettingsScreen(vm)
                }
                if(vm.busy) CircularProgressIndicator(Modifier.align(Alignment.TopCenter))
            }
        }
    }
}
