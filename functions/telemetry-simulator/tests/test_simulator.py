import io
import json
import unittest
from datetime import datetime, timedelta, timezone
from unittest.mock import MagicMock
from urllib.error import HTTPError, URLError

from simulator import NoRedirect, Settings, run_once

POINT = "11111111-1111-4111-8111-111111111111"
MEASUREMENT = "22222222-2222-4222-8222-222222222222"


class SimulatorTests(unittest.TestCase):
    def settings(self, **changes):
        return Settings.from_env({"MONITORING_POINT_ID": POINT, **changes})

    def response(self, status="COMPLETED"):
        opener = MagicMock()
        response = io.BytesIO(json.dumps({"id": MEASUREMENT, "evaluationStatus": status}).encode())
        response.status = 201
        opener.open.return_value = response
        return opener

    def test_default_dry_run_does_not_send(self):
        opener = MagicMock()
        result = run_once(self.settings(), opener=opener)
        self.assertEqual("DRY_RUN", result["mode"])
        opener.open.assert_not_called()

    def test_post_matches_contract_and_converts_to_utc(self):
        opener = self.response()
        now = datetime(2026, 10, 29, 10, 0, tzinfo=timezone(timedelta(hours=-5)))
        run_once(self.settings(SIMULATOR_DRY_RUN="false", SIMULATED_TEMPERATURE="9.25"),
                 now=now, opener=opener)
        request = opener.open.call_args.args[0]
        self.assertEqual("http://localhost:8081/api/measurements", request.full_url)
        self.assertEqual("POST", request.method)
        self.assertEqual({"monitoringPointId": POINT, "temperature": 9.25,
                          "timestamp": "2026-10-29T15:00:00Z"}, json.loads(request.data))
        self.assertEqual(5, opener.open.call_args.kwargs["timeout"])

    def test_all_evaluation_states_are_preserved(self):
        for status in ("COMPLETED", "PENDING", "RULE_NOT_CONFIGURED"):
            with self.subTest(status=status):
                result = run_once(self.settings(SIMULATOR_DRY_RUN="false"), opener=self.response(status))
                self.assertEqual(status, result["evaluationStatus"])

    def test_transport_failure_is_not_retried(self):
        for error in (URLError("offline"), TimeoutError()):
            with self.subTest(error=type(error).__name__):
                opener = MagicMock()
                opener.open.side_effect = error
                with self.assertRaisesRegex(RuntimeError, "resultado desconocido"):
                    run_once(self.settings(SIMULATOR_DRY_RUN="false"), opener=opener)
                self.assertEqual(1, opener.open.call_count)

    def test_http_failures_are_not_retried_or_logged_with_body(self):
        for code in (400, 404, 422, 500, 503):
            with self.subTest(code=code):
                opener = MagicMock()
                opener.open.side_effect = HTTPError("https://internal", code, "secret", {}, None)
                with self.assertRaisesRegex(RuntimeError, f"HTTP {code}; sin reintento"):
                    run_once(self.settings(SIMULATOR_DRY_RUN="false"), opener=opener)
                self.assertEqual(1, opener.open.call_count)

    def test_configuration_rejects_invalid_inputs_before_network(self):
        for changes in ({"MONITORING_POINT_ID": "invalid"}, {"SIMULATED_TEMPERATURE": "nan"},
                        {"SIMULATED_TEMPERATURE": "inf"}, {"HTTP_TIMEOUT_SECONDS": "0"},
                        {"HTTP_TIMEOUT_SECONDS": "31"}, {"HTTP_TIMEOUT_SECONDS": "nan"},
                        {"SIMULATOR_DRY_RUN": "yes"}, {"TELEMETRY_BASE_URL": "ftp://host"},
                        {"TELEMETRY_BASE_URL": "https://user:secret@host"},
                        {"TELEMETRY_BASE_URL": "https://host/path"}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.settings(**changes)

    def test_naive_datetime_is_rejected(self):
        with self.assertRaises(ValueError):
            run_once(self.settings(), now=datetime(2026, 10, 29))

    def test_unexpected_status_is_rejected(self):
        opener = self.response()
        opener.open.return_value.status = 200
        with self.assertRaisesRegex(RuntimeError, "distinto de 201"):
            run_once(self.settings(SIMULATOR_DRY_RUN="false"), opener=opener)

    def test_invalid_response_is_not_reported_as_success(self):
        with self.assertRaisesRegex(RuntimeError, "inválida"):
            run_once(self.settings(SIMULATOR_DRY_RUN="false"), opener=self.response("UNKNOWN"))

    def test_redirect_does_not_forward_post(self):
        self.assertIsNone(NoRedirect().redirect_request(None, None, 307, "", {}, "https://other"))


if __name__ == "__main__":
    unittest.main()
