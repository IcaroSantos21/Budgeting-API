import { Component, OnInit, inject, signal } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';
import { formatBRL } from '../../utils/money';

@Component({
  selector: 'app-total',
  standalone: true,
  imports: [],
  templateUrl: './total.html',
  styleUrl: './total.css'
})
export class TotalComponent implements OnInit {
  private transactionService = inject(TransactionService);
  total = signal<number | null>(null);
  error = signal<string | null>(null);

  ngOnInit(): void {
    this.transactionService.getTotal().subscribe({
      next: (sum) => this.total.set(sum.total),
      error: (err) => this.error.set('Erro ao buscar o total: ' + err.message)
    });
  }

  formatBRL = formatBRL;
}
