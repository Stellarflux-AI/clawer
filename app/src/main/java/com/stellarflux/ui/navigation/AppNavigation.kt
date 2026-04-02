package com.stellarflux.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.stellarflux.data.repository.OpenClawRepository
import com.stellarflux.data.repository.ProjectRepository
import com.stellarflux.data.repository.ServerRepository
import com.stellarflux.ui.screens.agents.AgentsScreen
import com.stellarflux.ui.screens.chat.ChatScreen
import com.stellarflux.ui.screens.servers.ServersScreen
import com.stellarflux.ui.screens.skills.SkillsScreen
import com.stellarflux.ui.screens.settings.SettingsScreen

object Routes {
    const val CHAT = "chat"
    const val CHAT_WITH_KEY = "chat/{sessionKey}"
    const val SERVERS = "servers"
    const val AGENTS = "agents"
    const val SKILLS = "skills"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    serverRepository: ServerRepository,
    projectRepository: ProjectRepository,
    openClawRepository: OpenClawRepository,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.CHAT,
        modifier = modifier
    ) {
        composable(Routes.CHAT) {
            ChatScreen(
                sessionKey = null,
                openClawRepository = openClawRepository,
                onOpenDrawer = onOpenDrawer,
                onNavigateToAgents = { navController.navigate(Routes.AGENTS) },
                onNavigateToSkills = { navController.navigate(Routes.SKILLS) }
            )
        }
        composable(
            Routes.CHAT_WITH_KEY,
            arguments = listOf(navArgument("sessionKey") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionKey = backStackEntry.arguments?.getString("sessionKey")
            ChatScreen(
                sessionKey = sessionKey,
                openClawRepository = openClawRepository,
                onOpenDrawer = onOpenDrawer,
                onNavigateToAgents = { navController.navigate(Routes.AGENTS) },
                onNavigateToSkills = { navController.navigate(Routes.SKILLS) }
            )
        }
        composable(Routes.SERVERS) {
            ServersScreen(
                serverRepository = serverRepository,
                openClawRepository = openClawRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.AGENTS) {
            AgentsScreen(
                openClawRepository = openClawRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SKILLS) {
            SkillsScreen(
                openClawRepository = openClawRepository,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToServers = { navController.navigate(Routes.SERVERS) },
                onNavigateToAgents = { navController.navigate(Routes.AGENTS) },
                onNavigateToSkills = { navController.navigate(Routes.SKILLS) }
            )
        }
    }
}
