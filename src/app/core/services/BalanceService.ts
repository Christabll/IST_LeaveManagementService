import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';

export interface LeaveBalanceDto {
  leaveType: string | any;
  defaultBalance: number;
  usedLeave: number;
  remainingLeave: string;
  carryOver: number;
}

@Injectable({
  providedIn: 'root'
})
export class BalanceService {
  private balanceSubject = new BehaviorSubject<{ userId: string; balance: LeaveBalanceDto[] }>({ userId: '', balance: [] });
  balance$ = this.balanceSubject.asObservable();
  private adminApiUrl = environment.adminApiUrl;

  constructor(private http: HttpClient) {}

  setUserBalance(userId: string, balance: LeaveBalanceDto[]) {
    this.balanceSubject.next({ userId, balance });
  }

  updateBalance(userId: string, balance: LeaveBalanceDto[]) {
    this.setUserBalance(userId, balance);
  }

  clearBalance() {
    this.balanceSubject.next({ userId: '', balance: [] });
  }

  private getHeaders() {
    const token = localStorage.getItem('token');
    return {
      headers: new HttpHeaders({
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      })
    };
  }

  adjustUsedDays(userId: string, leaveTypeId: string | number, usedDays: number, reason?: string): Observable<any> {
    const url = `${this.adminApiUrl}/adjust-balance`;
    const payload = {
      userId,
      leaveTypeId,
      newBalance: null,
      usedDays,
      reason: reason || 'Manual adjustment of used days'
    };
    
    return this.http.post<any>(url, payload, this.getHeaders());
  }
  
  adjustUsedDaysV2(userId: string, leaveTypeId: string | number, usedDays: number, reason?: string): Observable<any> {
    const url = `${this.adminApiUrl}/adjust-used-days`;
    const payload = {
      userId,
      leaveTypeId,
      usedDays,
      reason: reason || 'Manual adjustment of used days'
    };
    
    return this.http.post<any>(url, payload, this.getHeaders());
  }
} 