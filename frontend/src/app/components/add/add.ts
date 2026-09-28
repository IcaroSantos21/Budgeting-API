import { Component, signal, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TransactionService } from '../../services/transaction.service';
import { Category } from '../../models/transaction.model';

@Component({
  selector: 'app-add',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './add.html',
  styleUrl: './add.css'
})
export class AddComponent {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);

  categories: Category[] = ['GROCERIES', 'PHARMA', 'AUTO'];
  success = signal(false);
  error = signal<string | null>(null);

  form = this.fb.group({
    description: ['', Validators.required],
    category: ['GROCERIES' as Category, Validators.required],
    amountInReais: [0, [Validators.required, Validators.min(0.01)]]
  });

  submit(): void {
    if (this.form.invalid) {
      return;
    }

    this.success.set(false);
    this.error.set(null);

    const { description, category, amountInReais } = this.form.getRawValue();
    const amountInCents = Math.round((amountInReais ?? 0) * 100);

    this.transactionService.create({
      description: description!,
      category: category as Category,
      amount: amountInCents
    }).subscribe({
      next: () => {
        this.success.set(true);
        this.form.reset({ description: '', category: 'GROCERIES', amountInReais: 0 });
      },
      error: (err) => this.error.set('Erro ao criar transação: ' + err.message)
    });
  }
}