import { Injectable, inject } from "@angular/core";
import { HttpClient } from "@angular/common/http";
import { Observable } from "rxjs";
import { environment } from "../../environments/environment";
import { Transaction, TransactionRequest, Sum, Category } from "../models/transaction.model";

@Injectable({
    providedIn: 'root'
})
export class TransactionService {
    private http = inject(HttpClient);
    private baseUrl = `${environment.apiUrl}/transactions`;

    create(request: TransactionRequest): Observable<Transaction> {
        return this.http.post<Transaction>(this.baseUrl, request);
    }

    listByCategory(category: Category): Observable<Transaction[]> {
        return this.http.get<Transaction[]>(`${this.baseUrl}/${category}`);
    }

    getTotal(): Observable<Sum> {
        return this.http.get<Sum>(`${this.baseUrl}/total`);
    }

    getTotalByCategory(category: Category): Observable<Sum> {
        return this.http.get<Sum>(`${this.baseUrl}/${category}/total`);
    }

    sendAudio(audio: Blob): Observable<Blob> {
        const formData = new FormData();
        formData.append('file', audio, 'recording.webm');
        return this.http.post(`${this.baseUrl}/ai`, formData, { responseType: 'blob' });
    }
}