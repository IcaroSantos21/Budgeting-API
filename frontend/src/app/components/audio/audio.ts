import { Component, signal, inject } from '@angular/core';
import { TransactionService } from '../../services/transaction.service';

@Component({
  selector: 'app-audio',
  standalone: true,
  imports: [],
  templateUrl: './audio.html',
  styleUrl: './audio.css'
})
export class AudioComponent {
  private transactionService = inject(TransactionService);

  private mediaRecorder: MediaRecorder | null = null;
  private audioChunks: Blob[] = [];

  recording = signal(false);
  sending = signal(false);
  responseUrl = signal<string | null>(null);
  error = signal<string | null>(null);

  async startRecording(): Promise<void> {
    this.error.set(null);
    this.responseUrl.set(null);

    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      this.mediaRecorder = new MediaRecorder(stream);
      this.audioChunks = [];

      this.mediaRecorder.ondataavailable = (event) => this.audioChunks.push(event.data);

      this.mediaRecorder.onstop = () => {
        stream.getTracks().forEach(track => track.stop());
        this.sendRecording();
      };

      this.mediaRecorder.start();
      this.recording.set(true);
    } catch {
      this.error.set('Não foi possível acessar o microfone.');
    }
  }

  stopRecording(): void {
    this.mediaRecorder?.stop();
    this.recording.set(false);
  }

  private sendRecording(): void {
    const audioBlob = new Blob(this.audioChunks, { type: 'audio/webm' });
    this.sending.set(true);

    this.transactionService.sendAudio(audioBlob).subscribe({
      next: (responseBlob) => {
        this.sending.set(false);
        this.responseUrl.set(URL.createObjectURL(responseBlob));
      },
      error: (err) => {
        this.sending.set(false);
        this.error.set('Erro ao enviar áudio: ' + err.message);
      }
    });
  }
}