@file:JvmName("SynchronizedExecutors")

package dev.jsinco.hoarder.utilities

import dev.jsinco.hoarder.Hoarder
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity

private fun Long.coerce(): Long {
    return if (this < 1) 1 else this
}

fun Entity.sync(runnable: Runnable): ScheduledTask? {
    return this.scheduler.run(Hoarder.getInstance(), { runnable.run() }, null)
}

fun Location.sync(runnable: Runnable): ScheduledTask {
    return Bukkit.getRegionScheduler().run(Hoarder.getInstance(), this) { runnable.run() }
}

fun Block.sync(runnable: Runnable): ScheduledTask {
    return Bukkit.getRegionScheduler().run(Hoarder.getInstance(), this.location) { runnable.run() }
}

fun Entity.syncTimer(delay: Long, period: Long, runnable: Consumer<ScheduledTask>): ScheduledTask? {
    return this.scheduler.runAtFixedRate(Hoarder.getInstance(), runnable, null, delay.coerce(), period)
}

fun Collection<Entity>.syncTimer(delay: Long, period: Long, runnable: Consumer<ScheduledTask>): ScheduledTask? {
    return if (this.isEmpty()) {
        null
    } else {
        this.first().scheduler.runAtFixedRate(Hoarder.getInstance(), runnable, null, delay.coerce(), period)
    }
}

fun Location.syncTimer(delay: Long, period: Long, runnable: Consumer<ScheduledTask>): ScheduledTask {
    return Bukkit.getRegionScheduler().runAtFixedRate(Hoarder.getInstance(), this, runnable, delay.coerce(), period)
}

fun Entity.syncDelayed(delay: Long, runnable: Consumer<ScheduledTask>): ScheduledTask? {
    return this.scheduler.runDelayed(Hoarder.getInstance(), runnable, null, delay.coerce())
}

fun Location.syncDelayed(delay: Long, runnable: Consumer<ScheduledTask>): ScheduledTask {
    return Bukkit.getRegionScheduler().runDelayed(Hoarder.getInstance(), this, runnable, delay.coerce())
}

fun Block.syncDelayed(delay: Long, runnable: Consumer<ScheduledTask>): ScheduledTask {
    return Bukkit.getRegionScheduler().runDelayed(Hoarder.getInstance(), this.location, runnable, delay.coerce())
}


object Executors {

    private fun Long.asMillis() = this * 50

    @JvmStatic
    fun async(runnable: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getAsyncScheduler().runNow(Hoarder.getInstance()) { task ->
            try {
                runnable.accept(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun asyncTimer(delay: Long, period: Long, runnable: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getAsyncScheduler().runAtFixedRate(Hoarder.getInstance(), Consumer { task ->
            try {
                runnable.accept(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delay.asMillis(), period.asMillis(), TimeUnit.MILLISECONDS)
    }

    @JvmStatic
    fun asyncTimer(timeUnit: TimeUnit, delay: Long, period: Long, consumer: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getAsyncScheduler().runAtFixedRate(Hoarder.getInstance(), Consumer { task ->
            try {
                consumer.accept(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delay, period, timeUnit)
    }

    @JvmStatic
    fun asyncDelayed(delay: Long, runnable: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getAsyncScheduler().runDelayed(Hoarder.getInstance(), Consumer { task ->
            try {
                runnable.accept(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delay.asMillis(), TimeUnit.MILLISECONDS)
    }

    @JvmStatic
    fun asyncDelayed(timeUnit: TimeUnit, delay: Long, consumer: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getAsyncScheduler().runDelayed(Hoarder.getInstance(), Consumer { task ->
            try {
                consumer.accept(task)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, delay, timeUnit)
    }

    @JvmStatic
    fun global(runnable: Runnable): ScheduledTask {
        return Bukkit.getGlobalRegionScheduler().run(Hoarder.getInstance()) { runnable.run() }
    }

    @JvmStatic
    fun globalTimer(delay: Long, period: Long, consumer: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(Hoarder.getInstance(), consumer, delay.coerce(), period.coerce())
    }

    @JvmStatic
    fun globalDelayed(delay: Long, consumer: Consumer<ScheduledTask>): ScheduledTask {
        return Bukkit.getGlobalRegionScheduler().runDelayed(Hoarder.getInstance(), consumer, delay.coerce())
    }

    fun <T> (() -> T).async(): ScheduledTask {
        return async { this() }
    }

    fun <T> (() -> T).global(): ScheduledTask {
        return global { this() }
    }
}