package com.example.liveness.core.tasks

import com.example.liveness.core.DetectionTask
import com.google.mlkit.vision.face.Face

class RightFacingDetectionTask : DetectionTask {

    companion object {
        private const val RIGHT_FACING_THRESHOLD = 18f
    }

    override fun start() {
    }

    override fun process(face: Face): Boolean {
        val yaw = face.headEulerAngleY
        return yaw > RIGHT_FACING_THRESHOLD
    }
}