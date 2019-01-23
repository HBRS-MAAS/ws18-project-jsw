[![Codacy Badge](https://api.codacy.com/project/badge/Grade/7e250960350b4dba8b4e031e3d2b2918)](https://app.codacy.com/app/ssmabidi/ws18-project-jsw?utm_source=github.com&utm_medium=referral&utm_content=HBRS-MAAS/ws18-project-jsw&utm_campaign=Badge_Grade_Dashboard)
[![Build Status](https://travis-ci.org/HBRS-MAAS/ws18-project-jsw.svg?branch=master)](https://travis-ci.org/HBRS-MAAS/ws18-project-jsw)
[![Coverage Status](https://coveralls.io/repos/github/HBRS-MAAS/ws18-project-jsw/badge.svg?branch=master)](https://coveralls.io/github/HBRS-MAAS/ws18-project-jsw?branch=master)

# MAAS Project - JSW

Add a brief description of your project. Make sure to keep this README updated, particularly on how to run your project from the **command line**.

## Team Members
* Janhavi Puranik - [@janhavi19](https://github.com/janhavi19)
* Widya Aulia - [@widyaaulia](https://github.com/widyaaulia)

## Dependencies
* JADE v.4.5.0
* Java 8
* Gradle

## How to run
Just install gradle and run:

* Default scenario is small.

	gradle run

* To change scenario, put the name of the scenario name in the argument, e.g. scenario = 5-big-bakeries-100-days. 
    
	./gradlew run --args='5-big-bakeries-100-days'

It will automatically get the dependencies and start JADE with the configured agents.
In case you want to clean you workspace run

    gradle clean

## Eclipse
To use this project with eclipse run

    gradle eclipse


## Visualisation
Following are the steps to view the order status using our gui
* Select the customer name of the one you want to view the status
* Select the order id
* If you find 'O' in your status label that means particular task has been completed otherwise not
* To exit, you can press the Exit button. It will shutdown all agents.


