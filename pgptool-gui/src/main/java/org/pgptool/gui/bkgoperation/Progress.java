/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.pgptool.gui.bkgoperation;

import java.math.BigInteger;

import com.google.common.base.Preconditions;

/**
 * Class to report progress of operation.
 * 
 * Use {@link #create(String, ProgressHandler)} to instantiate it. Note that as
 * a result instance of {@link Updater} will be returned. You'll update progress
 * through this helper class
 * 
 * @author Sergey Karpushin
 *
 */
public class Progress {
	private String operationCode;
	private String stepCode;
	private Object[] stepArgs;

	private BigInteger totalSteps;
	private BigInteger stepsTaken;
	private Integer percentage;
	private boolean isCompleted;

	private long startedAt;
	private Long toBeFinishedBy;
	private ProgressHandler progressHandler;
	private boolean isCancelationRequested;

	public static Updater create(String operationCode, ProgressHandler progressHandler) {
		Progress ret = new Progress(operationCode, progressHandler);
		return ret.new Updater();
	}

	protected Progress(String operationCode, ProgressHandler progressHandler) {
		Preconditions.checkArgument(operationCode != null);
		Preconditions.checkArgument(progressHandler != null);

		this.operationCode = operationCode;
		this.progressHandler = progressHandler;
		startedAt = System.currentTimeMillis();
	}

	public String getOperationCode() {
		return operationCode;
	}

	public String getStepCode() {
		return stepCode;
	}

	public Object[] getStepArgs() {
		return stepArgs;
	}

	public BigInteger getTotalSteps() {
		return totalSteps;
	}

	public BigInteger getStepsTaken() {
		return stepsTaken;
	}

	public long getStartedAt() {
		return startedAt;
	}

	public Integer getPercentage() {
		return percentage;
	}

	public Long getToBeFinishedBy() {
		return toBeFinishedBy;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public void requestCancelation() {
		isCancelationRequested = true;
	}

	public class Updater {
		protected Updater() {
		}

		public boolean isCancelationRequested() {
			return isCancelationRequested;
		}

		public void updateStepsTaken(BigInteger stepsTaken) {
			Preconditions.checkState(totalSteps != null);
			Preconditions.checkArgument(stepsTaken != null);
			Progress.this.stepsTaken = stepsTaken;

			isCompleted = stepsTaken.equals(totalSteps);
			percentage = isCompleted ? 100 : stepsTaken.multiply(BigInteger.valueOf(100)).divide(totalSteps).intValue();
			updateEta();
			notifyHandler();
		}

		public void updateTotalSteps(BigInteger totalSteps) {
			Progress.this.totalSteps = totalSteps;
			updateEta();
			notifyHandler();
		}

		public void updateStepInfo(String stepCode, Object... stepArgs) {
			Progress.this.stepCode = stepCode;
			Progress.this.stepArgs = stepArgs;
			updateEta();
			notifyHandler();
		}

		private void notifyHandler() {
			progressHandler.onProgressUpdated(Progress.this);
		}

		private void updateEta() {
			if (stepsTaken == null || totalSteps == null) {
				return;
			}

			long elapsed = System.currentTimeMillis() - startedAt;
			toBeFinishedBy = BigInteger.valueOf(elapsed).multiply(totalSteps).divide(stepsTaken).longValue();
		}
	}
}
