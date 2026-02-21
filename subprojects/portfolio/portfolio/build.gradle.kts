/*
 *  Copyright 2025 Budapest University of Technology and Economics
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
plugins {
    id("java-common")
    id("kotlin-common")
    id("application")
}

group = "hu.bme.mit.theta"
version = "0.1-SNAPSHOT"

application {
    mainClass.set("hu.bme.mit.theta.portfolio.MainPortfolioKt")
    applicationName = "thetaport"
}

dependencies {
    implementation(Deps.Kotlin.stdlib)
    implementation("org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}")
    implementation(Deps.jcommander)
    implementation(project(":theta-common"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}
