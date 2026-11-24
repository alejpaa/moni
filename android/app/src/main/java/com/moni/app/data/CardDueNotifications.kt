package com.moni.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.composables.icons.lucide.R as LucideR
import com.moni.app.MainActivity
import io.github.jan.supabase.auth.auth
import java.io.IOException
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

internal object CardDueNotifications {
    const val CHANNEL_ID = "card_due_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vencimiento de tarjetas",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Recordatorios antes del día de pago de tus tarjetas de crédito"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Next occurrence of [dueDay] (1..31) on or after [today], clamped to each month's length. */
    fun nextDueDate(dueDay: Int, today: LocalDate): LocalDate {
        val day = dueDay.coerceIn(1, 31)
        val currentMonth = YearMonth.from(today)
        val inCurrentMonth = currentMonth.atDay(minOf(day, currentMonth.lengthOfMonth()))
        if (!inCurrentMonth.isBefore(today)) return inCurrentMonth
        val nextMonth = currentMonth.plusMonths(1)
        return nextMonth.atDay(minOf(day, nextMonth.lengthOfMonth()))
    }

    fun notifyCardDue(context: Context, account: Account, daysUntilDue: Int) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val title = when (daysUntilDue) {
            0 -> "Pago de tarjeta hoy"
            1 -> "Pago de tarjeta mañana"
            else -> "Próximo pago de tarjeta"
        }
        val body = when (daysUntilDue) {
            0 -> "Tu tarjeta ${account.name} vence hoy"
            1 -> "Tu tarjeta ${account.name} vence en 1 día"
            else -> "Tu tarjeta ${account.name} vence en $daysUntilDue días"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            account.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(LucideR.drawable.lucide_ic_credit_card)
            .setColor(0xFF9C8CD9.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(account.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently.
        }
    }
}

internal object CardDueScheduler {
    private const val PERIODIC_WORK_NAME = "card_due_daily_check"
    private const val IMMEDIATE_WORK_NAME = "card_due_immediate_check"

    private fun constraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun sync(context: Context, enabled: Boolean) {
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(PERIODIC_WORK_NAME)
            manager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<CardDueWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints())
            .build()
        manager.enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun triggerCheckNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<CardDueWorker>()
            .setConstraints(constraints())
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }
}

class CardDueWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Prefs.init(applicationContext)
        if (!Prefs.cardNotificationsEnabled.value) return Result.success()
        if (!CardDueNotifications.hasPermission(applicationContext)) return Result.success()

        return try {
            supabase.auth.awaitInitialization()
            if (supabase.auth.currentSessionOrNull() == null) return Result.success()

            val accounts = AccountRepository().list(includeArchived = false)
            val daysBefore = Prefs.cardNotifyDaysBefore.value
            val today = LocalDate.now()

            accounts
                .filter { it.type == "credit_card" && !it.archived && it.dueDay != null }
                .forEach { card ->
                    val dueDay = card.dueDay ?: return@forEach
                    val dueDate = CardDueNotifications.nextDueDate(dueDay, today)
                    val daysUntil = ChronoUnit.DAYS.between(today, dueDate).toInt()
                    if (daysUntil == daysBefore || daysUntil == 0) {
                        val milestoneKey = "$dueDate:$daysUntil"
                        if (!Prefs.wasCardNotifSent(card.id, milestoneKey)) {
                            CardDueNotifications.notifyCardDue(applicationContext, card, daysUntil)
                            Prefs.markCardNotifSent(card.id, milestoneKey)
                        }
                    }
                }
            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.success()
        }
    }
}
