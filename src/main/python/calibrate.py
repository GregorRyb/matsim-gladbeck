#!/usr/bin/env python
# -*- coding: utf-8 -*-

import os

import pandas as pd
import geopandas as gpd
import numpy as np

try:
    # Use the matsim package if available
    from matsim import calibration
except:
    # Alternatively, import calibration.py from same directory
    import calibration

# %%

if os.path.exists("mid.csv"):
    srv = pd.read_csv("mid.csv")
    sim = pd.read_csv("sim.csv")

    _, adj = calibration.calc_adjusted_mode_share(sim, srv)

    print(srv.groupby("mode").sum())

    print("Adjusted")
    print(adj.groupby("mode").sum())

    adj.to_csv("mid_adj.csv", index=False)

# %%

modes = ["walk", "car", "ride", "pt", "bike"]
fixed_mode = "walk"

# Initial ASCs
initial = {
    "bike": -2.3,
    "pt": 0,
    "car": 0,
    "ride": -4.12
}

# Modal split target
target = {
    "walk":  0.21,
    "bike":  0.1,
    "pt":    0.08,
    "car":   0.46,
    "ride":  0.15
}

region = gpd.read_file("../scenarios/metropole-ruhr-v1.0/shape/dilutionArea.shp").set_crs("EPSG:25832")


def f(persons):
    df = gpd.sjoin(persons.set_crs("EPSG:25832"), region, how="inner", op="intersects")
    return df


def adjust_trips(df):
    df = df[df.main_mode != "freight"]

    # Assign all intermodal pt trips to pt as main mode
    df.loc[df.main_mode.str.startswith("pt_"), "main_mode"] = "pt"

    return df


study, obj = calibration.create_mode_share_study("calib", "matsim-gladbeck-1.0-SNAPSHOT.jar",
                                        "../input/gladbeck-v1.0-25pct.config.xml",
                                        modes, target,
                                        initial_asc=initial,
                                        args="--25ct",
                                        jvm_args="-Xmx68G -Xmx68G -XX:+AlwaysPreTouch",
                                        person_filter=f, map_trips=filter_freight)


# %%

study.optimize(obj, 10)
