@file:Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")

package com.luisete.queda

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.luisete.queda.core.data.household.FirebaseHouseholdManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

interface AppRootPresenter {
    @Composable fun Render()
}

class HouseholdAppRootPresenter
    @Inject
    constructor(private val manager: FirebaseHouseholdManager) : AppRootPresenter {
        @Composable
        override fun Render() {
            LaunchedEffect(Unit) { manager.start() }
            HouseholdEntry(manager)
        }
    }

@Module
@InstallIn(SingletonComponent::class)
interface AppRootPresenterModule {
    @Binds fun bind(impl: HouseholdAppRootPresenter): AppRootPresenter
}
