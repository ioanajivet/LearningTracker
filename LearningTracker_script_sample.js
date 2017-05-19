
var metricNames = ['Sessions per week',
'Average length of a session',
'Average time between sessions',
'Forum sessions',
'Weekly assessment answers submitted',
'Timeliness of weekly assessment submission'];
var metricUnits = ['h',
'',
'',
'',
'',
'h'];

var values = [0.3,0,0,7,12,92];
var lastWeek = [8.9,3,11,14,69,72];
var thisWeek = [10.3,10,22,23,73,70];

function getSeriesValue(chart, i) {
	
	if(chart.points[i].series.name == 'You')
		return values[chart.x/60];
	else if(chart.points[i].series.name == 'Average graduate last week')
		return lastWeek[chart.x/60];
	else 
		return thisWeek[chart.x/60];
}

function timeStamp() {
  var now = new Date();
  var date = [ now.getFullYear(), now.getMonth() + 1, now.getDate() ];
  var time = [ now.getHours(), now.getMinutes(), now.getSeconds() ];

  for ( var i = 1; i < 3; i++ ) {
    if ( time[i] < 10 ) {
      time[i] = "0" + time[i];
    }
  }

  if( date[1] < 10 ) {
	date[1] = "0" + date[1];
  }
  
  return date.join("-") + "Z" + time.join(":");
}

function loadWidget() {
	
	var user_id = '123451';
	
	$('#container').highcharts({
		chart: {
			polar: true,
			style: {
				fontFamily: 'Open Sans, sans-serif'
			},
			type: 'area',
			events: {
				load: function () {
					var category = user_id + '_week8';
					gaPC('send', 'event', category, 'load_' + timeStamp());
				}
			}
		},

		exporting: {
            chartOptions: { // specific options for the exported image
                plotOptions: {
                    series: {
                        dataLabels: {
                            enabled: true
                        }
                    }
                }
            },
            scale: 3,
            fallbackToExportServer: true
        },
		
		title: {
			text: 'Learning tracker',
			style: {
				align: 'left'
			}
		},

		credits: {
			enabled: false
		},

		legend: {
			reversed: true
		},

		pane: {
			startAngle: 0,
			endAngle: 360
		},

		xAxis: {
			tickInterval: 60,
			min: 0,
			max: 360,
			labels: {
				formatter: function () {
					return metricNames[this.value/60];
				}
			},
			gridLineWidth: 1
		},

		yAxis: {
			min: 0,
			max: 10,
			gridLineWidth: 1,
			labels: {
				enabled: false
			},
			tickPositions: [0, 5, 10],
			visible: true
		},

		plotOptions: {
			series: {
				allowPointSelect: true,
				pointStart: 0,
				pointInterval: 60,
				cursor: 'pointer',
				marker: {
					symbol: 'diamond',
					radius: 3
				}
			},
			column: {
				pointPadding: 0,
				groupPadding: 0
			}
		},

		tooltip: {
			shared: true,
			formatter: function () {
				var tooltip_text = '<b>' + metricNames[this.x/60] + '</b>';
				var unit = metricUnits[this.x/60];

				for (i = this.points.length - 1; i >= 0; i--) { 
					tooltip_text += '<br/>' + this.points[i].series.name + ': <b>' + getSeriesValue(this, i) + ' ' + unit + '</b>';
				}

				return tooltip_text;
			},
		},
		
		series: [		
		{
			type: 'line',
			name: 'Average graduate this week',
			color: 'rgba(188, 64, 119, 0.5)',
			data: [2.5,	3.7,	2.1,	2,	7.2,	3.7],
			visible: false,
			events: {
				show: function () {
					gaPC('send', 'event', user_id + '_week8', 'show-this-week_' + timeStamp());
				},
				hide: function () {
					gaPC('send', 'event', user_id + '_week8', 'hide-this-week_' + timeStamp());
				}
			}
		},

		{
			name: 'Average graduate last week',
			color: 'rgba(255, 255, 102, 0.5)',
            data: [2.6,	4,	2.1,	1.9,	8.2,	3.6
], 
			events: {
				show: function () {
					gaPC('send', 'event', user_id + '_week8', 'show-last-week_' + timeStamp());
				},
				hide: function () {
					gaPC('send', 'event', user_id + '_week8', 'hide-last-week_' + timeStamp());
				}
			}

		},

		{
			name: 'You',
			color: 'rgba(144, 202, 249, 0.5)',
			data: [7.4,	3.3,	1.2,	3.4,	7.4,	0.5




],
			events: {
				show: function () {
					gaPC('send', 'event', user_id + '_week8', 'show-you_' + timeStamp());
				},
				hide: function () {
					gaPC('send', 'event', user_id + '_week8', 'hide-you_' + timeStamp());
				}
			}
		}]
	});
}
		
loadWidget();