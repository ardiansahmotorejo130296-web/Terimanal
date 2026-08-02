package com.example

import org.junit.Test
import java.io.File
import java.nio.file.Files

class CpuFreqBenchmarkTest {
    @Test
    fun benchmark() {
        val tempDir = Files.createTempDirectory("cpufreq").toFile()
        val cores = 8
        for (i in 0 until cores) {
            val cpuDir = File(tempDir, "cpu$i/cpufreq")
            cpuDir.mkdirs()
            File(cpuDir, "cpuinfo_max_freq").writeText("2400000\n")
            File(cpuDir, "scaling_cur_freq").writeText("1200000\n")
        }

        // Warm up
        for (i in 0 until 100) {
            baseline(tempDir, cores)
            optimized(tempDir, cores)
        }

        val iterations = 1000
        val baselineTime = measureTime {
            for (i in 0 until iterations) {
                baseline(tempDir, cores)
            }
        }

        val optimizedTime = measureTime {
            for (i in 0 until iterations) {
                optimized(tempDir, cores)
            }
        }

        println("Baseline: ${baselineTime / 1_000_000.0} ms")
        println("Optimized: ${optimizedTime / 1_000_000.0} ms")
        println("Improvement: ${(baselineTime - optimizedTime).toDouble() / baselineTime * 100}%")

        tempDir.deleteRecursively()
    }

    private inline fun measureTime(block: () -> Unit): Long {
        val start = System.nanoTime()
        block()
        return System.nanoTime() - start
    }

    private fun baseline(dir: File, cores: Int): Double {
        var totalMax = 0L
        var totalCur = 0L
        for (i in 0 until cores) {
            val maxFile = File(dir, "cpu$i/cpufreq/cpuinfo_max_freq")
            val curFile = File(dir, "cpu$i/cpufreq/scaling_cur_freq")
            if (maxFile.exists() && curFile.exists()) {
                val max = maxFile.readText().trim().toLongOrNull() ?: 0L
                val cur = curFile.readText().trim().toLongOrNull() ?: 0L
                if (max > 0) {
                    totalMax += max
                    totalCur += cur
                }
            }
        }
        return if (totalMax > 0) (totalCur.toDouble() / totalMax.toDouble()) * 100.0 else 0.0
    }

    private fun optimized(dir: File, cores: Int): Double {
        var totalMax = 0L
        var totalCur = 0L
        val buf = ByteArray(32)
        for (i in 0 until cores) {
            val maxFile = File(dir, "cpu$i/cpufreq/cpuinfo_max_freq")
            val curFile = File(dir, "cpu$i/cpufreq/scaling_cur_freq")

            // Note: skipping exists() checks and just catching Exception if files don't exist
            // which avoids a stat() syscall if we just try to open it. But for benchmarking we keep it simple.
            try {
                val max = readLongFast(maxFile, buf)
                if (max > 0) {
                    val cur = readLongFast(curFile, buf)
                    totalMax += max
                    totalCur += cur
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        return if (totalMax > 0) (totalCur.toDouble() / totalMax.toDouble()) * 100.0 else 0.0
    }

    private fun readLongFast(file: File, buf: ByteArray): Long {
        file.inputStream().use { it.read(buf) }
        var result = 0L
        for (b in buf) {
            val c = b.toInt().toChar()
            if (c in '0'..'9') {
                result = result * 10 + (c - '0')
            } else {
                break
            }
        }
        return result
    }
}
