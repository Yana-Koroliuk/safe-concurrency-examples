using csharp;

public class Program
{
    private const int NumAccounts = 1_000;
    private const int NumOperations = 100_000;
    private const int InitBalance = 1_000;
    
    private static async Task Main()
    {
        await using var exchangeHelper = new ExchangeHelper(NumAccounts, InitBalance);

        var producers = Enumerable.Range(0, NumOperations)
            .Select(_ => exchangeHelper.RunRandomTransferAsync(Random.Shared, NumAccounts))
            .ToArray();

        await Task.WhenAll(producers);

        Console.WriteLine($"Σ = {exchangeHelper.GetTotalBalance()}");
    }
}
