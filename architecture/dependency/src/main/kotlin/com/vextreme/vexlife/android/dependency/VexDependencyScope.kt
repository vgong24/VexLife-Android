package com.vextreme.vexlife.android.dependency

import android.app.Service

/**
 * Type-level lifetime topology for dependency construction.
 *
 * These types own no runtime behavior. They make a dependency's lifetime relationship visible in
 * source so a human or AI can trace parent/sibling/child composition without inferring scope from
 * package placement or whichever Android object happened to construct the dependency.
 */
interface VexDependencyScope

interface VexRootDependencyScope :
    VexDependencyScope

interface VexChildDependencyScope<
    Parent : VexDependencyScope,
> :
    VexDependencyScope

/** Android-process lifetime. */
sealed interface VexApplicationScope :
    VexRootDependencyScope

/** Retained VexLife UI/session lifetime, currently realized by ViewModel. */
sealed interface VexSessionScope :
    VexChildDependencyScope<
        VexApplicationScope
    >

/**
 * One concrete Android Service child lifetime.
 *
 * The actual Service type remains in the generic binding so Playback, Navigation, Call, and future
 * sibling Service scopes cannot be silently swapped while still appearing as the same dependency
 * scope.
 */
sealed interface VexServiceScope<
    ServiceType : Service,
> :
    VexChildDependencyScope<
        VexApplicationScope
    >

// [VXG RealForever][VEXARCHITECTURE][DEPENDENCY][TYPED-SCOPES]
