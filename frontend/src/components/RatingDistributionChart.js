import React from "react";
import { Bar } from "react-chartjs-2";
import { Chart, BarElement, CategoryScale, LinearScale, Tooltip } from "chart.js";

Chart.register(BarElement, CategoryScale, LinearScale, Tooltip);

function RatingDistributionChart({ distribution }) {
  // distribution: { "0.5": 1, "1.0": 2, ..., "5.0": 3 }
  const labels = [
    "0.5", "1.0", "1.5", "2.0", "2.5",
    "3.0", "3.5", "4.0", "4.5", "5.0"
  ];
  const dataArr = labels.map(label => distribution[label] || 0);
  const maxValue = Math.max(...dataArr);

  const barColors = dataArr.map((v, i) =>
    v === maxValue
      ? "#e64980" // 최대값: 진한색
      : "#f8bbd0" // 나머지: 연한색
  );

  const labelColors = labels.map((label, i) =>
    (i === 0 || i === labels.length - 1) ? "#bdbdbd" : "#222"
  );

  const data = {
    labels,
    datasets: [
      {
        data: dataArr,
        backgroundColor: barColors,
        borderRadius: 6,
        borderSkipped: false,
        barPercentage: 0.7,
        categoryPercentage: 0.9,
      },
    ],
  };

  const options = {
    plugins: {
      legend: { display: false },
      tooltip: {
        callbacks: {
          label: ctx => `${ctx.parsed.y}명`
        }
      }
    },
    scales: {
      x: {
        grid: { display: false },
        ticks: {
          color: (ctx) => labelColors[ctx.index],
          font: { size: 16, weight: "bold" }
        }
      },
      y: {
        beginAtZero: true,
        grid: { display: false },
        ticks: { display: false }
      }
    },
    animation: { duration: 600 },
    responsive: true,
    maintainAspectRatio: false,
  };

  // 최대값 위에 숫자 표시
  const maxIndex = dataArr.indexOf(maxValue);

  return (
    <div style={{ height: 160, position: "relative" }}>
      <Bar data={data} options={options} />
      {maxValue > 0 && (
        <div
          style={{
            position: "absolute",
            left: `calc(${(maxIndex / (labels.length - 1)) * 100}% - 10px)` ,
            top: 10,
            color: "#e64980",
            fontWeight: "bold",
            fontSize: 22,
            pointerEvents: "none",
            zIndex: 2,
          }}
        >
          {maxValue}
        </div>
      )}
    </div>
  );
}

export default RatingDistributionChart; 