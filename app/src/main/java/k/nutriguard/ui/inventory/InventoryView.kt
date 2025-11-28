@file:OptIn(
    ExperimentalMaterial3Api::class
) // Basically opting into experimental images

package k.nutriguard.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import k.nutriguard.domain.Allergen
import k.nutriguard.viewmodel.InventoryItem
import k.nutriguard.viewmodel.InventoryViewModel
import k.nutriguard.domain.FoodItem
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.core.content.ContextCompat
import k.nutriguard.domain.UserProfile
import k.nutriguard.ui.scanner.takePhotoAndScanBarcode


//Color Constants underneath:
private val BgSurface   = Color(0xFF0E0E0E)
private val CardSurface = Color(0xFF1C1C1C)
private val Divider     = Color(0xFF2A2A2A)
private val NutriYellow = Color(0xFFFFD200)
private val ChipRed     = Color(0xFFE74C3C)



@Composable // Main inventory screen it takes ViewModel + User Profile
fun InventoryView(vm: InventoryViewModel, user: UserProfile) {
    val state by vm.uiState.collectAsState() // Takes the state from the viewModel as Compose state

    //load the users cart from state
    LaunchedEffect(user.personalCartId) {
        vm.loadUserCart(user.personalCartId)
    }


    var showCamera by remember { mutableStateOf(false) } // whether to show barcode overlay or not
    var isFormVisible by remember { mutableStateOf(false) } // whether the manual "add item" form is visible
    // Text field state for custom item form
    var manualName by remember { mutableStateOf("") }
    var manualExpiration by remember { mutableStateOf("") }
    var manualIngredients by remember { mutableStateOf("") }
    // Allergens state for custom item
    var selectedAllergens by remember { mutableStateOf(setOf<Allergen>()) }
    var allergenDropdownExpanded by remember { mutableStateOf(false) }

    // Root container for the whole screen
    Box(
        modifier = Modifier
            .fillMaxSize() // fill the whole screen and make it background colour of BgSurface
            .background(BgSurface)
    ) {
        LazyColumn( // Make it so it is vertically scrollable with a LazyColumn with search + inventory items
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search bar row at the top row
            item {
                SearchBarRow( // call the composable to render the image
                    value = state.query, //bind search query
                    onValueChange = vm::onQueryChange,
                    onSearch = vm::searchNow,
                    onClear = {
                        vm.onQueryChange("") // reset the query
                        vm.clearResults() // clear the results
                    },
                    onBarcodeClick = { showCamera = true } // if the icon is clicked make showCamera = true
                )
            }
            // Loading indicator when a search is running
            if (state.isSearching) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator(color = NutriYellow) } // Comes from Compose's Material 3 library (done where with the OPT-in at the top)
                }
            } // Search results row, if any
            else if (state.results.isNotEmpty()) {
                item {
                    Text(
                        "Results",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                    // Horizontal scrolling in order to make it better for individuals to see all the results
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.results) { food: FoodItem ->
                            SearchCard(food = food) { vm.addMock(food, user.personalCartId) } //add this food item to users cart if they click
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            // Inventory Items which have been already added
            items(state.items, key = { it.id }) { item ->
                InventoryCard(
                    item = item,
                    // Remove item from the user's personal cart
                    onDelete = { vm.deleteItem(item.id, user.personalCartId) }
                )
            }
            item { Spacer(Modifier.height(96.dp)) }
        }


         //Barcode scanner overlay
        if (showCamera) {
            BarcodeCameraScreen( // show the composable helper
                onBarcodeScanned = { code -> // after being scanned turn the showCamera to false
                    showCamera = false
                    vm.onBarcodeScanned(code) //ViewModel performs searchByBarcode after we get the code
                },
                onClose = { showCamera = false } //user backs outs without using scanner
            )
        }

        // Custom item model form overlay
        if (isFormVisible) {
            //Dimmed background behind the dialog
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Card dialog itself
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Food name input
                        TextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Food Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        Spacer(Modifier.height(12.dp))

                        // Allergens dropdown multiselect
                        ExposedDropdownMenuBox(
                            expanded = allergenDropdownExpanded,
                            onExpandedChange = { allergenDropdownExpanded = it }
                        ) {
                            // Read only Text-Field that shows the selected allergens
                            TextField(
                                value = when {
                                    selectedAllergens.isEmpty() -> ""
                                    selectedAllergens.size == Allergen.values().size -> "All allergens selected"
                                    else -> selectedAllergens.joinToString(", ") { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } }
                                },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Allergens") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            // the actual dropdown menu
                            ExposedDropdownMenu(
                                expanded = allergenDropdownExpanded,
                                onDismissRequest = { allergenDropdownExpanded = false }
                            ) {
                                // Select all row
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Checkbox(
                                                checked = selectedAllergens.size == Allergen.values().size,
                                                onCheckedChange = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text("Select All")
                                        }
                                    },
                                    onClick = {
                                        selectedAllergens =
                                            if (selectedAllergens.size == Allergen.values().size)
                                                emptySet()
                                            else
                                                Allergen.values().toSet()
                                    }
                                )
                                HorizontalDivider()
                                // One checkable row per allergen enum value
                                Allergen.values().forEach { allergen ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Checkbox(
                                                    checked = selectedAllergens.contains(allergen),
                                                    onCheckedChange = null
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = allergen.name.replace("_", " ")
                                                        .lowercase()
                                                        .replaceFirstChar { c -> c.uppercase() }
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedAllergens =
                                                if (selectedAllergens.contains(allergen))
                                                    selectedAllergens - allergen
                                                else
                                                    selectedAllergens + allergen
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        // Ingredients input, comma-seperated
                        TextField(
                            value = manualIngredients,
                            onValueChange = { manualIngredients = it },
                            label = { Text("Relevant Ingredients") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(Modifier.height(12.dp))
                        // Expiration date text input
                        TextField(
                            value = manualExpiration,
                            onValueChange = { manualExpiration = it },
                            label = { Text("Expiration Date (yyyy/MM/dd)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )

                        Spacer(Modifier.height(16.dp))

                        // Buttons row: Cancel & Confirm
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = {
                                manualName = ""
                                manualExpiration = ""
                                manualIngredients = ""
                                selectedAllergens = emptySet()
                                allergenDropdownExpanded = false
                                isFormVisible = false
                            }) {
                                Text("Cancel")
                            }
                            Spacer(Modifier.width(8.dp))

                            // Confirm: build items and then send to ViewModel
                            Button(onClick = {
                                // split ingredients by comma into a list
                                val ingredientsList = manualIngredients.trim()
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .toMutableList()

                                vm.addCustomItem(
                                    manualName.trim(),
                                    selectedAllergens,
                                    ingredientsList,
                                    manualExpiration.trim(),
                                    cartId = user.personalCartId
                                )

                                // Reset fields and close dialog
                                manualName = ""
                                manualExpiration = ""
                                manualIngredients = ""
                                selectedAllergens = emptySet()
                                allergenDropdownExpanded = false
                                isFormVisible = false
                            }) {
                                Text("Confirm")
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button: opens the manual add-item form
        FloatingActionButton(
            onClick = { isFormVisible = true },
            containerColor = NutriYellow,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Text("+", fontWeight = FontWeight.Black, fontSize = 28.sp)
        }
    }
}


// Search Bar
@Composable
private fun SearchBarRow(
    value: String, // current text in search field
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBarcodeClick: () -> Unit // invoke when barcodeIcon is pressed
) {
    Row( //makes a row that is shaped like a search bar by filling the width
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, Divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Text input for search query
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Search foods…", color = Color(0xFF8E8E8E)) },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = NutriYellow,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.weight(1f) // take all remaining row width
        )
        // creates the search button and searches using viewModel
        IconButton(onClick = onSearch) {
            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = NutriYellow)
        }
        // Clear input button and clears using Viewmodel function
        IconButton(onClick = onClear) {
            Icon(Icons.Outlined.Clear, contentDescription = "Clear", tint = Color(0xFFBDBDBD))
        }
        // Barcode scanner button
        IconButton(onClick = onBarcodeClick) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan barcode", tint = NutriYellow)
        }
    }
}


// Search Card for results from the API
@Composable
private fun SearchCard(food: FoodItem // food result from OpenFoodFacts
                       , onClick: () -> Unit) { // called when user clicks the elements
    Column( // want it in a column format
        modifier = Modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurface)
            .border(1.dp, Divider, RoundedCornerShape(12.dp))
            .padding(8.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // uses coil from dependencies and import to create an Async Food Image from the API
        AsyncImage(
            model = food.imageUrl,
            contentDescription = food.name,
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(Modifier.height(6.dp))
        // Food Name
        Text(food.name, color = Color(0xFFBDBDBD), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        // Allergens string
        Text(
            text =
                if (food.allergens.isEmpty()) "No allergens"
                else food.allergens.joinToString(", ") { it.name },
            color = Color(0xFFBDBDBD),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


//Inventory Item Card
@Composable
private fun InventoryCard(item: InventoryItem, onDelete: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Divider, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Top Row has image name and delete button for the card
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.food.imageUrl,
                    contentDescription = item.food.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // Delete icon
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0x22222222))
                        .border(1.dp, Divider, CircleShape)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFB85C5C)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // List the Allergens on the card in a text line after analyzing the food item
            Text(
                text = when {
                    item.allergies.isEmpty() -> "Allergens: No allergens"
                    item.allergies.size == Allergen.values().size -> "Allergens: All allergens selected"
                    else -> "Allergens: ${item.allergies.joinToString(", ") { it.name.replace("_", " ").lowercase().replaceFirstChar { c -> c.uppercase() } }}"
                },
                color = Color(0xFFBDBDBD),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))
            // Same thing with Allergens happens for ingredients too
            Text(
                text =
                    if (item.ingredients.isEmpty()) "Ingredients: No Ingredients"
                    else "Ingredients: ${item.ingredients.joinToString(", ")}",
                color = Color(0xFFBDBDBD),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(8.dp))

            // food item is expired use the Food domain model to see if its expired
            if (item.food.isExpired()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(ChipRed)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("⚠ Expired", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
            }

            // Displays when expired
            val expiryText = item.food.expirationDate.takeIf { it.isNotBlank() } ?: "N/A"
            Text(
                text = "Expires: $expiryText",
                color = Color(0xFFBDBDBD),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun BarcodeCameraScreen(
    onBarcodeScanned: (String) -> Unit, // what we do when we successfully scan a barcode
    onClose: () -> Unit                 // what happens when the user leaves the screen
) {
    val context = LocalContext.current              // get the current Android context (needed for CameraX + ML Kit)
    val lifecycleOwner = LocalLifecycleOwner.current // needed so CameraX knows when to start/stop
    val permission = Manifest.permission.CAMERA      // the string for camera permission

    // Whether we have camera permission right now
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    // This is what pops up the permission request popup
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission = granted                 // update permission status
            if (!granted) onClose()                // if they say no → close the screen
        }

    // When this composable loads for the first time, ask for permission if needed
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(permission)
    }

    // If we STILL don’t have permission, show a message and stop loading the camera
    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Camera permission is required to scan barcodes.")
        }
        return // stop here, don't load the camera
    }

    // The actual camera preview UI
    val previewView = remember { PreviewView(context) }

    // We will store the ImageCapture use-case here (used for snapping a pic)
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    // Ask Android for access to the camera hardware (async)
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    // Executor to run camera callbacks on the main UI thread
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    // When the cameraProvider is ready, set up the preview + capture functions
    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get() // get the camera provider

        // Build the live camera preview screen
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider) // attach preview to our view
        }

        // Build the ImageCapture object (takes pictures fast, low latency)
        val imgCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        imageCapture = imgCapture // store it so we can use it later

        // Use the back camera (normal phone camera)
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll() // remove previous camera sessions if any

            // Bind everything so the camera actually works
            cameraProvider.bindToLifecycle(
                lifecycleOwner,    // lifecycle (start/stop automatically)
                cameraSelector,    // back camera
                preview,           // live preview
                imgCapture         // picture-taking feature
            )
        } catch (e: Exception) {
            Log.e("BarcodeCamera", "Use case binding failed", e)
        }
    }

    // The actual UI layout of the screen
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan barcode") }, // screen title
                navigationIcon = {
                    IconButton(onClick = onClose) { // back arrow button
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // padding from the scaffold
        ) {
            // Show the live camera feed on the screen
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { previewView } // camera preview view
            )

            // The button at the bottom of the screen that takes the picture
            Button(
                onClick = {
                    val imgCap = imageCapture ?: return@Button // if imageCapture is null, exit

                    // Use our helper function to take a picture AND scan it
                    takePhotoAndScanBarcode(
                        context = context,
                        imageCapture = imgCap,
                        onBarcodeScanned = { rawValue ->
                            onBarcodeScanned(rawValue) // send barcode back to parent screen
                            // You can close the camera after scanning if you want
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter) // button stays at bottom center
                    .padding(16.dp)                // breathing room
            ) {
                Text("Capture") // button text
            }
        }
    }
}

