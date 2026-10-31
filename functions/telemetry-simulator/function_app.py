"""Azure Functions Python v2; configuración validada en cada ejecución."""

import logging
import azure.functions as func
from simulator import Settings, run_once

app = func.FunctionApp()


@app.timer_trigger(schedule="%SIMULATOR_SCHEDULE%", arg_name="timer",
                   run_on_startup=False, use_monitor=True)
def emit_measurement(timer: func.TimerRequest) -> None:
    if timer.past_due:
        logging.warning("simulator timer_past_due=true; no se reconstruyen lecturas históricas")
    run_once(Settings.from_env())
