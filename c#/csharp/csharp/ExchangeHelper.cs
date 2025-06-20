using System.Threading.Channels;

namespace csharp;

public readonly record struct TransferMessage(int SrcAccountIdx, int DestAccountIdx, int Amount);

public sealed class ExchangeHelper : IAsyncDisposable
{
    private const int MinAmount  = 1;
    private const int MaxAttempts = 3;
    private const int MaxAmount = 50; 
    
    private readonly int[] _balances;
    private readonly Channel<(TransferMessage Req, TaskCompletionSource<bool> Ack)> _mailBox;
    private readonly Task _actorTask;

    public ExchangeHelper(int accountNumber, int initialBalance)
    {
        _balances = Enumerable.Repeat(initialBalance, accountNumber).ToArray();
        _mailBox = Channel.CreateUnbounded<(TransferMessage, TaskCompletionSource<bool>)>(
            new UnboundedChannelOptions { SingleReader = true });
        _actorTask = Task.Run(ProcessLoopAsync);
    }
    
    public int  GetTotalBalance() => _balances.Sum();
    
    public async Task<bool> RunRandomTransferAsync(Random rnd, int numAccounts) 
    {
        for (var attempt = 0; attempt < MaxAttempts; attempt++)
        {
            var (srcAccountIdx, destAccountIdx) = PickDistinctPair(rnd, numAccounts);
            int amount = rnd.Next(MinAmount, MaxAmount);

            if (await TransferAsync(srcAccountIdx, destAccountIdx, amount))
                return true;  
        }
        return false;   
    }
    
    public async ValueTask DisposeAsync()
    {
        _mailBox.Writer.TryComplete();
        await _actorTask.ConfigureAwait(false);
    }
    
    private ValueTask<bool> TransferAsync(int srcAccountIdx, int destAccountIdx, int amount)
    {
        var completionSource = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
        _mailBox.Writer.TryWrite((new TransferMessage(srcAccountIdx, destAccountIdx, amount), completionSource));
        return new ValueTask<bool>(completionSource.Task);
    }
    
    private async Task ProcessLoopAsync()
    {
        await foreach (var (message, completionSource) in _mailBox.Reader.ReadAllAsync())
        {
            bool isValidTransfer = message.SrcAccountIdx != message.DestAccountIdx 
                                   && message.Amount > 0 && _balances[message.SrcAccountIdx] >= message.Amount;
            if (isValidTransfer)
            {
                _balances[message.SrcAccountIdx] -= message.Amount;
                _balances[message.DestAccountIdx] += message.Amount;
            }
            completionSource.TrySetResult(isValidTransfer);
        }
    }
    
    private (int Src, int Dst) PickDistinctPair(Random rnd, int limit)
    {
        int srcAccountIdx = rnd.Next(limit);
        int destAccountIdx;
        do { destAccountIdx = rnd.Next(limit); } while (destAccountIdx == srcAccountIdx);
        return (srcAccountIdx, destAccountIdx);
    }
}
