            this.balanceService.adjustUsedDaysV2(
                  request.userId, 
                  request.leaveTypeId, 
                  request.usedDays, 
                  request.reason
            ).subscribe({ 