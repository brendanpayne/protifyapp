package com.protify.protifyapp

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

class AboutActivity {

    data class DevProfile(
        var name: String,
        var linkedIn: String,
        var github: String,
        var email: String,
        var desc: String,
        var image: Int
    )

    private val devs = listOf(
        DevProfile(
            name = "Tom McReynolds",
            linkedIn = "https://www.linkedin.com/in/tommymcreynolds/",
            github = "",
            email = "",
            desc = "Backend Developer",
            image = R.drawable.tom
        ),
        DevProfile(
            name = "Brendan Payne",
            linkedIn = "https://www.linkedin.com/in/payneb2/",
            github = "",
            email = "",
            desc = "Full Stack Developer",
            image = R.drawable.brendan
        ),
        DevProfile(
            name = "Tyler Malovrh",
            linkedIn = "https://www.linkedin.com/in/tyler-malovrh-8b99a1205/",
            github = "",
            email = "",
            desc = "Frontend Developer",
            image = R.drawable.tyler
        ),
        DevProfile(
            name = "Cole Kramer",
            linkedIn = "https://www.linkedin.com/in/colekramer000/",
            github = "",
            email = "",
            desc = "Security Analyst",
            image = R.drawable.cole
        ),
        DevProfile(
            name = "Trever Adkins",
            linkedIn = "https://www.linkedin.com/in/trever-adkins-649921148/",
            github = "",
            email = "",
            desc = "Security Analyst",
            image = R.drawable.trever
        )
    )

    @Composable
    fun AboutPage(navController: NavController) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                AboutHeader(onBackClick = { navController.popBackStack() })
                AboutContent()
            }
        }
    }

    @Composable
    private fun AboutContent() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Protify",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            )
            DevProfiles()
            Text(
                text = "Made with ❤️ for UC IT Expo 2024",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            )
        }
    }

    @Composable
    private fun DevProfiles() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Meet the Team",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth()
            )
            devs.forEach { dev ->
                DevDetails(dev)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    @Composable
    private fun DevDetails(devProfile: DevProfile) {
        val context = LocalContext.current
        ElevatedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(devProfile.linkedIn))
                startActivity(context, intent, null)
            },
            modifier = Modifier.fillMaxWidth(0.8f),
            shape = RoundedCornerShape(8.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 0.dp
            )
        ){
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = devProfile.image),
                    contentDescription = "Developer Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Text(
                    text = devProfile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
                Text(
                    text = devProfile.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                )
            }
        }
    }


    @Composable
    private fun AboutHeader(onBackClick: () -> Unit){
        Surface(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onBackClick() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                // This is a placeholder to push the title to the center
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }

    @Preview
    @Composable
    private fun PreviewAboutPage() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AboutPage(navController = rememberNavController())
        }
    }
}