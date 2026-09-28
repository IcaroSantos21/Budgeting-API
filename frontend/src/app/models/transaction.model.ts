export type Category = 'GROCERIES' | 'PHARMA' | 'AUTO';

export interface TransactionRequest {
  description: string;
  category: Category;
  amount: number;
}

export interface Transaction {
  id: string;
  description: string;
  category: string;
  amount: number;
}

export interface Sum {
  total: number;
}
