import ExchangeHelper.Companion.pickDistinctPair
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.actor
import java.util.concurrent.ThreadLocalRandom

object ExchangeConfig {
    const val NUM_ACCOUNTS = 1_000
    const val NUM_OPERATIONS = 100_000
    const val INIT_BALANCE = 1_000

    const val MIN_AMOUNT = 1
    const val BOUND = 50
    const val MAX_ATTEMPTS = 3
}

data class TransferMessage(
    val srcAccountIdx: Int,
    val destAccountIdx: Int,
    val amount: Int,
    val completionSignal: CompletableDeferred<Boolean>
)

class ExchangeHelper(scope: CoroutineScope, accountNumber: Int, initBalance: Int) {

    private val balances = IntArray(accountNumber) { initBalance }

    val inbox = scope.actor(Dispatchers.Default, Channel.UNLIMITED) { processTransfers(channel, balances) }

    private suspend fun processTransfers(channel: ReceiveChannel<TransferMessage>, balances: IntArray) {
        for (msg in channel) {
            val isValidTransfer = msg.srcAccountIdx != msg.destAccountIdx &&
                    balances[msg.srcAccountIdx] >= msg.amount

            if (isValidTransfer) {
                balances[msg.srcAccountIdx] -= msg.amount
                balances[msg.destAccountIdx] += msg.amount
            }
            msg.completionSignal.complete(isValidTransfer)
        }
    }

    fun getTotalBalance(): Long = balances.fold(0L, Long::plus)

    companion object {
        fun pickDistinctPair(rnd: ThreadLocalRandom, limit: Int): Pair<Int, Int> {
            val srcAccountIdx = rnd.nextInt(limit)
            var destAccountIdx: Int
            do {
                destAccountIdx = rnd.nextInt(limit)
            } while (destAccountIdx == srcAccountIdx)
            return srcAccountIdx to destAccountIdx
        }
    }
}

fun main() = runBlocking {
    val exchangeHelper = ExchangeHelper(this, ExchangeConfig.NUM_ACCOUNTS, ExchangeConfig.INIT_BALANCE)
    val random = ThreadLocalRandom.current()

    val producers = List(ExchangeConfig.NUM_OPERATIONS) {
        launch(Dispatchers.Default) {
            repeat(ExchangeConfig.MAX_ATTEMPTS) { attempt ->
                val (srcAccountIdx, destAccountIdx) = pickDistinctPair(random, ExchangeConfig.NUM_ACCOUNTS)
                val amount = random.nextInt(ExchangeConfig.MIN_AMOUNT, ExchangeConfig.BOUND)
                val successSignal = CompletableDeferred<Boolean>()
                exchangeHelper.inbox.send(TransferMessage(srcAccountIdx, destAccountIdx, amount, successSignal))
                if (successSignal.await()) return@launch
            }
        }
    }
    producers.forEach { it.join() }

    exchangeHelper.inbox.close()
    (exchangeHelper.inbox as Job).join()

    println("Σ = ${exchangeHelper.getTotalBalance()}")
}
