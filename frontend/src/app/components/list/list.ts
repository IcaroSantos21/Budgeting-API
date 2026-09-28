import { Component, signal, inject } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';
import { Transaction, Category } from '../../models/transaction.model';
import { formatBRL } from '../../utils/money';

@Component({
  selector: 'app-list',
  standalone: true,
  imports: [],
  templateUrl: './list.html',
  styleUrl: './list.css'
})
export class ListComponent {
  private transactionService = inject(TransactionService);

  categories: Category[] = ['GROCERIES', 'PHARMA', 'AUTO'];
  selectedCategory = signal<Category>('GROCERIES');
  transactions = signal<Transaction[]>([]);
  error = signal<string | null>(null);

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category as Category);
  }

  search(): void {
    this.error.set(null);
    this.transactionService.listByCategory(this.selectedCategory()).subscribe({
      next: (list) => this.transactions.set(list),
      error: (err) => this.error.set('Erro ao buscar transações: ' + err.message)
    });
  }

  formatBRL = formatBRL;
}