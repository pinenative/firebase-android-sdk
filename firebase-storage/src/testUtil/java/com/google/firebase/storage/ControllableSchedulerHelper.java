// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.firebase.storage;

import android.annotation.SuppressLint;
import java.util.ArrayList;
import java.util.List;

/** for test purposes only. */
@SuppressLint("RestrictedApi")
public class ControllableSchedulerHelper extends StorageTaskScheduler {

  final List<Runnable> pausedUploadRunnables = new ArrayList<>();
  final List<Runnable> pausedDownloadRunnables = new ArrayList<>();
  final List<Runnable> pausedOtherRunnables = new ArrayList<>();
  int pauseCount = 0;
  Long callbackThread;

  public static synchronized ControllableSchedulerHelper getInstance() {
    if (!(StorageTaskScheduler.sInstance instanceof ControllableSchedulerHelper)) {
      StorageTaskScheduler.sInstance = new ControllableSchedulerHelper();
    }
    return (ControllableSchedulerHelper) sInstance;
  }

  public synchronized void pause() {
    pauseCount++;
  }

  public synchronized void resume() {
    pauseCount--;
    if (pauseCount == 0) {
      callbackThread = null;
      for (Runnable r : pausedUploadRunnables) {
        scheduleUpload(r);
      }
      for (Runnable r : pausedDownloadRunnables) {
        scheduleDownload(r);
      }
      for (Runnable r : pausedOtherRunnables) {
        scheduleCommand(r);
      }
      pausedUploadRunnables.clear();
      pausedDownloadRunnables.clear();
      pausedOtherRunnables.clear();
    }
  }

  /** Verify that all callbacks run on the same thread. */
  public synchronized void verifyCallbackThread() {
    if (callbackThread == null) {
      callbackThread = Thread.currentThread().getId();
    }

    if (callbackThread != Thread.currentThread().getId()) {
      /*
       * TODO(b/279463318): This should be removed in the future.
       * Previously, since we used to guarantee max one live thread at any given time,
       * this threw an error if the executor ran the callback on a new thread.
       * Since we no longer guarantee this, there's a chance that the executor may spawn a new thread if no tasks are pending.
       * The error remains for future debugging needs.
       */
      System.err.println("WARNING: Callback fired on unexpected callback thread.");
    }
  }

  @Override
  public synchronized void scheduleCommand(Runnable task) {
    if (pauseCount > 0) {
      pausedOtherRunnables.add(task);
      return;
    }
    super.scheduleCommand(task);
  }

  @Override
  public synchronized void scheduleUpload(Runnable task) {
    if (pauseCount > 0) {
      pausedUploadRunnables.add(task);
      return;
    }
    super.scheduleUpload(task);
  }

  @Override
  public synchronized void scheduleDownload(Runnable task) {
    if (pauseCount > 0) {
      pausedDownloadRunnables.add(task);
      return;
    }
    super.scheduleDownload(task);
  }
}
