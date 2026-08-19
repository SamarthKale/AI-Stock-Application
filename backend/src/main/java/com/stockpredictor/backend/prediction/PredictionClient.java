package com.stockpredictor.backend.prediction;

import com.stockpredictor.backend.common.PredictionServiceUnavailableException;
import com.stockpredictor.backend.common.dto.PredictionRequestDto;
import com.stockpredictor.backend.common.dto.PredictionResponseDto;

/** Seam over "how the backend calls the prediction service" — mirrors the
 *  FirebaseTokenVerifier/FirebaseAdminTokenVerifier interface+impl pattern already used for
 *  Firebase, so tests can substitute a fake without a live FastAPI process (see
 *  FakePredictionClient), same reasoning as why Phase 3's auth tests don't need a live
 *  Firebase project. */
public interface PredictionClient {

    /** @throws PredictionServiceUnavailableException if the prediction service is unreachable,
     *  times out, or errors — never a raw network exception. */
    PredictionResponseDto predict(PredictionRequestDto request);
}
