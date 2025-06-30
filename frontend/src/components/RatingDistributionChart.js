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
  const maxIndex = dataArr.indexOf(maxValue);

  const barColors = dataArr.map((v, i) =>
    v === maxValue && v > 0
      ? "#e64980" // 최대값: 진한색
      : "#f8bbd0" // 나머지: 연한색
  );

  // X축 라벨: 0.5, 5.0만 연하게, 나머지는 숨김
  const labelColors = labels.map((label, i) =>
    (i === 0 || i === labels.length - 1) ? "#bdbdbd" : "rgba(0,0,0,0)"
  );

  const labelFontSize = 16;

  // 실제로 평가자가 존재하는 구간의 인덱스만 추출
  const nonZeroIndexes = dataArr
    .map((v, i) => (v > 0 ? i : -1))
    .filter(i => i !== -1);
  const minNonZeroIndex = nonZeroIndexes.length > 0 ? Math.min(...nonZeroIndexes) : null;
  const maxNonZeroIndex = nonZeroIndexes.length > 0 ? Math.max(...nonZeroIndexes) : null;

  const data = {
    labels,
    datasets: [
      {
        data: dataArr,
        backgroundColor: barColors,
        borderRadius: 8,
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
        },
        backgroundColor: '#fff',
        titleColor: '#e64980',
        bodyColor: '#333',
        borderColor: '#e64980',
        borderWidth: 1,
        titleFont: { weight: 'bold', size: 16 },
        bodyFont: { size: 14 },
      }
    },
    scales: {
      x: {
        grid: { display: false, drawBorder: false },
        ticks: {
          color: (ctx) => labelColors[ctx.index],
          font: { size: labelFontSize, weight: "bold" },
          callback: function(value, index) {
            // 0.5, 5.0 라벨 숨김
            return '';
          },
          padding: 6,
        },
      },
      y: {
        beginAtZero: true,
        grid: { display: false, drawBorder: false },
        ticks: { display: false },
        min: 0,
      }
    },
    animation: { duration: 600 },
    responsive: true,
    maintainAspectRatio: false,
    layout: {
      padding: {
        top: 24,
        bottom: 0,
        left: 0,
        right: 0
      }
    }
  };

  return (
    <div
      style={{
        height: 120,
        width: "100%",
        maxWidth: 200, // .movie-detail-poster와 동일하게 제한
        minWidth: 0,
        position: "relative",
        background: "#fafafd",
        borderRadius: 12,
        padding: "8px 0 0 0",
        boxSizing: "border-box",
        overflow: "hidden",
        margin: "0 auto"
      }}
    >
      <Bar data={data} options={{
        ...options,
        scales: {
          ...options.scales,
          x: {
            ...options.scales.x,
            ticks: {
              ...options.scales.x.ticks,
              callback: function(value, index) {
                // 0.5, 5.0 라벨 숨김
                return '';
              },
            },
          },
        },
      }} />
      {/* 최대값 구간(진한색), 최소/최대 구간(회색)만 위에 숫자 표시 */}
      {labels.map((label, i) => {
        // 최대값 구간(핑크)
        if (i === maxIndex && maxValue > 0) {
          return (
            <div
              key={label}
              style={{
                position: "absolute",
                left: `calc(${(i / (labels.length - 1)) * 100}% )`,
                top: 2,
                color: "#e64980",
                fontWeight: "bold",
                fontSize: 16,
                pointerEvents: "none",
                zIndex: 2,
                textAlign: "center",
                width: 32,
                transform: "translateX(-50%)"
              }}
            >
              {label}
            </div>
          );
        }
        // 최소/최대 구간(회색, 단 최대값 구간과 겹치지 않을 때만)
        if ((i === minNonZeroIndex || i === maxNonZeroIndex) && i !== maxIndex) {
          return (
            <div
              key={label}
              style={{
                position: "absolute",
                left: `calc(${(i / (labels.length - 1)) * 100}% )`,
                top: 2,
                color: "#bdbdbd",
                fontWeight: "bold",
                fontSize: 16,
                pointerEvents: "none",
                zIndex: 2,
                textAlign: "center",
                width: 32,
                transform: "translateX(-50%)"
              }}
            >
              {label}
            </div>
          );
        }
        return null;
      })}
    </div>
  );
}

export default RatingDistributionChart; 