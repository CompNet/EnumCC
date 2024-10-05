EnumCC
===================
Efficient Enumeration of Correlation Clustering Optimal Solution Space

* Copyright 2020-21 Nejat Arınık

*EnumCC* is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation. For source availability and license information see the file `LICENCE`

* **Lab site:** http://lia.univ-avignon.fr/
* **GitHub repo:** https://github.com/CompNet/EnumCC
* **Contact:** Nejat Arınık <arinik9@gmail.com>, Vincent Labatut <vincent.labatut@univ-avignon.fr>

-------------------------------------------------------------------------

## Description
`EnumCC` is an optimal solution space enumeration method for the *Correlation Clustering (CC)* problem. It relies on two essential tasks: Recurrent neighborhood search (*RNS*) and *jumping* onto an undiscovered solution. The former is performed by the component `RNSCC`, whereas the latter is done by the commercial solver `Cplex`.

In the first step, instead of directly *jumping* onto undiscovered optimal solutions one by one through `Cplex`, as in a traditional sequential approach, its component `RNSCC` discovers the recurrent neighborhood of the current optimal solution *P* with the hope of discovering new optimal solutions. The recurrent neighborhood of an optimal solution *P*, represents the set of optimal solutions, reached directly or indirectly from *P* depending on the maximum distance parameter *maxNbEdit*. Whether a new solution is found or not through `RNSCC`, the jumping process into a new solution *P* is performed. If *P* is not empty, the workflow of `RNS` and *jumping* is repeated again. Otherwise, the enumeration process stops. See our article [[Arınık'23](#references)] for more details.

If you use this software, please cite article [[Arınık'23](#references)]:
```bibtex
@Article{Arinik2023,
  author    = {Arınık, Nejat and Figueiredo, Rosa and Labatut, Vincent},
  title     = {Efficient Enumeration of the Optimal Solutions to the Correlation Clustering problem},
  journal   = {Journal of Global Optimization},
  year      = {2023},
  volume    = {86},
  pages     = {355-391},
  doi       = {10.1007/s10898-023-01270-3},
}
```


## Input parameters
 * `inFile`: Input file path. See `in/exemple.G` for the input graph format. 

 * `outDir`: Output directory path. Default `.` (i.e. the current directory).

 * `initMembershipFilePath`: The membership file path, from which the `RNSCC` starts. It must be an optimal solution of the given signed graph. Moreover, It must be named as `membership0.txt` or something different than `membership<x>.txt`. See `out/exemple/membership0.txt` for its format. This file can be obtained through  [ExCC](https://github.com/CompNet/ExCC) by running the script `run-cp-bb.sh`.

 * `java.library.path`: The `Cplex` Java library path. It is usually found in `<YOUR_CPLEX_PATH>/cplex/lib/cplex.jar`.

 * `maxNbEdit`: The maximum value edit distance value to be considered in edit operations. We show in our experiments that `maxNbEdit=3` is usually more appropriate. 

 * `tilim`: Time limit in seconds for the whole program. Default `-1`, which means no time limit.
  
 * `solLim`: Maximum number of optimal solutions to be discovered. This can be useful when there is a huge number of optimal solutions, e.g. 50,000. Default `-1`.

 * `JAR_filepath_RNSCC`: The jar file path for `RNSCC`.

 * `LPFilePath`: It allows to import a `Cplex` LP file, corresponding to a ILP formulation of a signed graph for the CC problem. *Remark:* Such a file is obtained through Cplex by doing `exportModel()`. This file can be obtained through [ExCC](https://github.com/CompNet/ExCC) by running the script `run-cp-bb.sh`. In `ExCC`, the name of this file is `strengthedModelAfterRootRelaxation.lp`.


## Instructions & Use

### Use 1
Install [`IBM CPlex`](https://www.ibm.com/docs/en/icos/20.1.0?topic=2010-installing-cplex-optimization-studio). The default installation location is: `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>`. Tested with `Cplex` 12.8 and 20.1.

Put `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>/cplex/lib/cplex.jar` into the `lib` folder in this repository.

Compile and get the jar file for *RNSCC*: `ant -v -buildfile build-rns.xml compile jar`.

Compile and get the jar file for *EnumCC* `ant -v -buildfile build.xml compile jar`.

We need a starting optimal solution and the ILP model of the given signed graph. We can obtain them by running the script `run-cp-bb.sh` in the [ExCC](https://github.com/CompNet/ExCC) repository.

Run the script `run.sh`.


### Use 2
Put `/opt/ibm/ILOG/CPLEX_Studio<YOUR_VERSION>/cplex/lib/cplex.jar` into the `lib` folder in this repository.

Compile and get the jar file for `RNSCC`: `ant -v -buildfile build-rns.xml compile jar`.

Compile and get the jar file for `EnumCC`: `ant -v -buildfile build.xml compile jar`.

Download the [Sosocc](https://github.com/CompNet/Sosocc) repository and put these jar files into the `lib` folder. Then, run first `ExCC` and then `EnumCC(3)`. See the instructions of the `Sosocc` repository for more details.

Example command:
```
ant -v -buildfile build.xml compile jar
ant -v -buildfile build.xml -DinFile="in/example.G" -DoutDir="out/example" -DmaxNbEdit=3 -DinitMembershipFilePath="out/example/membership0.txt" -DLPFilePath="strengthedModelAfterRootRelaxation.lp" -DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=5000 run
```


## Output

* `<x>`: Folder `<x>`, where `x is a numerical value starting from 1. Each folder contains the result of a `RNSCC` execution and possesses one or multiple optimal solutions.
* `allResults.txt`: File storing all absolute paths of the discovered optimal solutions.
* `exec-time.txt`: Execution time for the whole enumeration process.
* `jump-exec-time<x>.txt`: Execution time for the `x`.th jumping process through `Cplex`.
* `jump-log<x>.txt`: `Cplex` log file regarding the the `x`.th jumping process.
* `jump-status<x>.txt`: The `Cplex` status result in the end of the jumoing process. Three values are possible: `Optimal`, `SolLim`, `Infeasible`.
* `membership<x>.txt`: The starting membership file for the (`x+1`).th `RNSCC` process.


## References
* **[Arınık'23]** N. Arınık & R. Figueiredo & V. Labatut. *Efficient Enumeration of the Optimal Solutions to the Correlation Clustering problem*, Journal of Global Optimization, 86:355-391, 2023. DOI: [10.1007/s10898-023-01270-3](http://doi.org/10.1007/s10898-023-01270-3) [⟨hal-03935831⟩](https://hal.archives-ouvertes.fr/hal-03935831)
