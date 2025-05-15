import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface LeaveBalanceDto {
  leaveType: string;
  defaultBalance: number;
  usedLeave: number;
  remainingLeave: string;
  carryOver: number;
  balance?: string;
}

@Injectable({
  providedIn: 'root'
})
export class BalanceService {
  private balanceSubject = new BehaviorSubject<{ userId: string, balance: LeaveBalanceDto[] }>({
    userId: '',
    balance: []
  });

  balance$ = this.balanceSubject.asObservable();

  updateBalance(userId: string, balanceData: LeaveBalanceDto[]) {
    const transformedBalance = balanceData.map(item => ({
      ...item,
      balance: item.balance || item.remainingLeave
    }));
    
    this.balanceSubject.next({ userId, balance: transformedBalance });
  }
} 